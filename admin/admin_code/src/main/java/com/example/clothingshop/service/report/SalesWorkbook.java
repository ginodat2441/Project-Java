package com.example.clothingshop.service.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class SalesWorkbook {
  private static final List<String> STATUSES = List.of(
      "Chờ xác nhận", "Đã xác nhận", "Đang giao", "Hoàn thành",
      "Đã hủy", "Trả hàng", "Giao thất bại");

  private final XSSFWorkbook book;
  private final CellStyle header;
  private final CellStyle integer;
  private final CellStyle date;

  private SalesWorkbook(XSSFWorkbook book) {
    this.book = book;
    header = book.createCellStyle();
    header.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
    header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    var font = book.createFont();
    font.setBold(true);
    header.setFont(font);
    header.setBorderBottom(BorderStyle.THIN);
    integer = book.createCellStyle();
    integer.setDataFormat(book.createDataFormat().getFormat("#,##0"));
    date = book.createCellStyle();
    date.setDataFormat(book.createDataFormat().getFormat("dd/mm/yyyy"));
  }

  public static byte[] write(Map<String, Object> report) throws IOException {
    try (var book = new XSSFWorkbook(); var buffer = new ByteArrayOutputStream()) {
      new SalesWorkbook(book).populate(report);
      book.write(buffer);
      return buffer.toByteArray();
    }
  }

  private void populate(Map<String, Object> report) {
    var sheet = book.createSheet("BÁO CÁO");
    sheet.setDisplayGridlines(false);
    sheet.createFreezePane(0, 5);
    widths(sheet, 8, 17, 24, 4, 8, 24, 15, 4, 8, 31, 24);
    sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));
    var title = cell(sheet, 0, 0, "BÁO CÁO THỐNG KÊ TÌNH HÌNH BÁN HÀNG");
    var titleStyle = book.createCellStyle();
    var titleFont = book.createFont();
    titleFont.setBold(true);
    titleFont.setFontHeightInPoints((short) 16);
    titleStyle.setFont(titleFont);
    title.setCellStyle(titleStyle);
    sheet.getRow(0).setHeightInPoints(25);
    cell(sheet, 1, 0, "Từ " + report.get("from") + " đến " + report.get("to"));
    cell(sheet, 2, 0, "Doanh thu ròng (đ)");
    cell(sheet, 2, 2, ((Map<?, ?>) report.get("totals")).get("NET_AFTER_RETURN"));
    cell(sheet, 3, 0,
        "Doanh thu theo ngày ghi nhận và người xử lý đơn. Trạng thái tính theo đơn tạo trong kỳ.");
    headers(sheet, 4, 0, "STT", "NGÀY", "DOANH THU RÒNG (đ)");
    headers(sheet, 4, 4, "STT", "TRẠNG THÁI", "SỐ ĐƠN");
    headers(sheet, 4, 8, "STT", "NHÂN VIÊN", "DOANH THU RÒNG (đ)");
    var days = rows(report, "series");
    for (int i = 0; i < days.size(); i++) {
      var day = days.get(days.size() - 1 - i);
      cell(sheet, 5 + i, 0, i + 1);
      cell(sheet, 5 + i, 1, LocalDate.parse(day.get("PERIOD").toString()));
      cell(sheet, 5 + i, 2, day.get("NET_AFTER_RETURN"));
    }
    var orders = rows(report, "orders");
    int[] counts = new int[STATUSES.size() + 1];
    orders.forEach(order -> counts[statusIndex(order.get("STATUS"))]++);
    int statusCount = counts.length - (counts[STATUSES.size()] == 0 ? 1 : 0);
    for (int i = 0; i < statusCount; i++) {
      cell(sheet, 5 + i, 4, i + 1);
      cell(sheet, 5 + i, 5, status(i));
      cell(sheet, 5 + i, 6, counts[i]);
    }
    var employees = rows(report, "employees");
    for (int i = 0; i < employees.size(); i++) {
      cell(sheet, 5 + i, 8, i + 1);
      cell(sheet, 5 + i, 9, employee(employees.get(i)));
      cell(sheet, 5 + i, 10, employees.get(i).get("REVENUE"));
    }
    if (!orders.isEmpty()) chart(sheet, true, 4, 5, 6, statusCount);
    if (!employees.isEmpty()) chart(sheet, false, 8, 9, 10, employees.size());
    if (orders.isEmpty()) cell(sheet, 6 + statusCount, 4, "Không có đơn trong kỳ");
    if (employees.isEmpty()) cell(sheet, 5, 9, "Không có doanh thu trong kỳ");

    var orderSheet = book.createSheet("ĐƠN HÀNG");
    widths(orderSheet, 21, 16, 22, 30, 18, 16, 16, 20);
    headers(orderSheet, 0, 0, "Mã đơn", "Ngày đặt", "Trạng thái", "Nhân viên",
        "Tiền hàng (đ)", "Giảm giá (đ)", "Phí giao (đ)", "Tổng thanh toán (đ)");
    for (int i = 0; i < orders.size(); i++) {
      var order = orders.get(i);
      row(orderSheet, i + 1, order.get("BILL_CODE"),
          LocalDate.parse(order.get("ORDER_DATE").toString()), status(statusIndex(order.get("STATUS"))),
          employee(order), order.get("SUB_TOTAL"), order.get("DISCOUNT_AMOUNT"),
          order.get("SHIPPING_FEE"), order.get("TOTAL_AMOUNT"));
    }
    filter(orderSheet, orders.size(), 7);

    var products = rows(report, "products");
    var productSheet = book.createSheet("SẢN PHẨM");
    widths(productSheet, 19, 37, 12, 12, 17, 16, 17, 19, 17, 17);
    headers(productSheet, 0, 0, "Mã", "Sản phẩm", "SL bán", "SL hoàn", "Tiền hàng (đ)",
        "Giảm giá (đ)", "Hàng hoàn (đ)", "Doanh thu ròng (đ)", "Giá vốn (đ)", "Lãi gộp (đ)");
    var keys = List.of("PRODUCT_CODE", "PRODUCT_NAME", "SOLD", "RETURNED", "GROSS", "DISCOUNT",
        "RETURN_VALUE", "NET_AFTER_RETURN", "COGS", "GROSS_PROFIT");
    for (int i = 0; i < products.size(); i++) {
      var product = products.get(i);
      row(productSheet, i + 1, keys.stream().map(product::get).toArray());
    }
    filter(productSheet, products.size(), 9);
  }

  private void chart(XSSFSheet sheet, boolean pie, int startColumn, int labels, int values, int size) {
    var drawing = sheet.createDrawingPatriarch();
    int top = size + 6;
    var chart = drawing.createChart(
        drawing.createAnchor(0, 0, 0, 0, startColumn, top, startColumn + 3, top + 15));
    chart.setTitleText(pie ? "Tỷ lệ đơn theo trạng thái" : "Doanh thu theo nhân viên");
    chart.setTitleOverlay(false);
    var categories = XDDFDataSourcesFactory.fromStringCellRange(
        sheet, new CellRangeAddress(5, size + 4, labels, labels));
    var numbers = XDDFDataSourcesFactory.fromNumericCellRange(
        sheet, new CellRangeAddress(5, size + 4, values, values));
    XDDFChartData data;
    if (pie) {
      chart.getOrAddLegend().setPosition(LegendPosition.BOTTOM);
      data = chart.createData(ChartTypes.PIE, null, null);
      data.setVaryColors(true);
    } else {
      var axis = chart.createValueAxis(AxisPosition.LEFT);
      axis.setNumberFormat("#,##0");
      data = chart.createData(ChartTypes.BAR, chart.createCategoryAxis(AxisPosition.BOTTOM), axis);
      ((XDDFBarChartData) data).setBarDirection(BarDirection.COL);
    }
    data.addSeries(categories, numbers).setTitle(pie ? "Số đơn" : "Doanh thu ròng (đ)", null);
    chart.plot(data);
  }

  private static int statusIndex(Object value) {
    int id = value instanceof Number n ? n.intValue() : -1;
    return id >= 0 && id < STATUSES.size() ? id : STATUSES.size();
  }

  private static String status(int index) {
    return index < STATUSES.size() ? STATUSES.get(index) : "Khác";
  }

  private static Object employee(Map<String, Object> order) {
    return order.get("EMPLOYEE") == null ? "Chưa gán nhân viên" : order.get("EMPLOYEE");
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> rows(Map<String, Object> report, String key) {
    return (List<Map<String, Object>>) report.get(key);
  }

  private static void widths(Sheet sheet, int... widths) {
    for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
  }

  private static void filter(Sheet sheet, int count, int endColumn) {
    sheet.createFreezePane(0, 1);
    sheet.setAutoFilter(new CellRangeAddress(0, count, 0, endColumn));
  }

  private void headers(Sheet sheet, int row, int start, String... labels) {
    for (int i = 0; i < labels.length; i++) cell(sheet, row, start + i, labels[i]).setCellStyle(header);
  }

  private void row(Sheet sheet, int row, Object... values) {
    for (int i = 0; i < values.length; i++) cell(sheet, row, i, values[i]);
  }

  private Cell cell(Sheet sheet, int row, int column, Object value) {
    var target = sheet.getRow(row);
    if (target == null) target = sheet.createRow(row);
    var cell = target.createCell(column);
    if (value instanceof LocalDate day) {
      cell.setCellValue(day);
      cell.setCellStyle(date);
    } else if (value instanceof Number amount) {
      cell.setCellValue(amount.doubleValue());
      cell.setCellStyle(integer);
    } else if (value != null) {
      cell.setCellValue(value.toString());
    }
    return cell;
  }
}
