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
  private final CellStyle[][] rowStyles = new CellStyle[2][3];
  private final CellStyle title;
  private final CellStyle totalLabel;
  private final CellStyle totalAmount;

  private SalesWorkbook(XSSFWorkbook book) {
    this.book = book;
    header = style(IndexedColors.DARK_TEAL, IndexedColors.WHITE, true, 11);
    header.setWrapText(true);
    title = style(IndexedColors.DARK_BLUE, IndexedColors.WHITE, true, 17);
    totalLabel = style(IndexedColors.PALE_BLUE, IndexedColors.DARK_BLUE, true, 11);
    totalAmount = style(IndexedColors.PALE_BLUE, IndexedColors.DARK_BLUE, true, 14);
    var formats = book.createDataFormat();
    short moneyFormat = formats.getFormat("#,##0;[Red](#,##0);0");
    short dateFormat = formats.getFormat("dd/mm/yyyy");
    totalAmount.setDataFormat(moneyFormat);
    for (int band = 0; band < rowStyles.length; band++) {
      var background = band == 0 ? IndexedColors.WHITE : IndexedColors.GREY_25_PERCENT;
      for (int type = 0; type < rowStyles[band].length; type++) {
        rowStyles[band][type] = style(background, IndexedColors.DARK_BLUE, false, 11);
      }
      rowStyles[band][1].setDataFormat(moneyFormat);
      rowStyles[band][2].setDataFormat(dateFormat);
      rowStyles[band][2].setAlignment(HorizontalAlignment.CENTER);
    }
  }

  private CellStyle style(IndexedColors background, IndexedColors foreground, boolean bold, int size) {
    var style = book.createCellStyle();
    style.setFillForegroundColor(background.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    var font = book.createFont();
    font.setFontName("Calibri");
    font.setColor(foreground.getIndex());
    font.setBold(bold);
    font.setFontHeightInPoints((short) size);
    style.setFont(font);
    return style;
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
    layout(sheet);
    sheet.createFreezePane(0, 5);
    widths(sheet, 8, 17, 24, 4, 8, 24, 15, 4, 8, 31, 24);
    banner(sheet, 0, 0, 10, "BÁO CÁO BÁN HÀNG", title);
    sheet.getRow(0).setHeightInPoints(33);
    var displayDate = java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu");
    banner(sheet, 1, 0, 10,
        "Kỳ báo cáo  " + LocalDate.parse(report.get("from").toString()).format(displayDate)
            + " – " + LocalDate.parse(report.get("to").toString()).format(displayDate), null);
    banner(sheet, 2, 0, 1, "Doanh thu ròng (đ)", totalLabel);
    cell(sheet, 2, 2, ((Map<?, ?>) report.get("totals")).get("NET_AFTER_RETURN"))
        .setCellStyle(totalAmount);
    var orders = rows(report, "orders");
    banner(sheet, 2, 4, 5, "Đơn đặt trong kỳ", totalLabel);
    cell(sheet, 2, 6, orders.size()).setCellStyle(totalAmount);
    sheet.getRow(2).setHeightInPoints(26);
    banner(sheet, 3, 0, 10,
        "Doanh thu theo ngày ghi nhận và người xử lý. Trạng thái tính theo đơn đặt trong kỳ.", null);
    headers(sheet, 4, 0, "STT", "Ngày", "Doanh thu ròng (đ)");
    headers(sheet, 4, 4, "STT", "Trạng thái đơn", "Số đơn");
    headers(sheet, 4, 8, "STT", "Nhân viên xử lý", "Doanh thu ròng (đ)");
    var days = rows(report, "series");
    for (int i = 0; i < days.size(); i++) {
      var day = days.get(days.size() - 1 - i);
      cell(sheet, 5 + i, 0, i + 1);
      cell(sheet, 5 + i, 1, LocalDate.parse(day.get("PERIOD").toString()));
      cell(sheet, 5 + i, 2, day.get("NET_AFTER_RETURN"));
    }
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
    layout(orderSheet);
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
    layout(productSheet);
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
    int top = Math.max(13, size + 6);
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

  private static void layout(Sheet sheet) {
    sheet.setDisplayGridlines(false);
    sheet.setDefaultRowHeightInPoints(19);
    sheet.setFitToPage(true);
    var print = sheet.getPrintSetup();
    print.setLandscape(true);
    print.setFitWidth((short) 1);
    print.setFitHeight((short) 0);
  }

  private void banner(Sheet sheet, int row, int start, int end, String text, CellStyle style) {
    sheet.addMergedRegion(new CellRangeAddress(row, row, start, end));
    for (int col = start; col <= end; col++) {
      var cell = cell(sheet, row, col, col == start ? text : null);
      if (style != null) cell.setCellStyle(style);
    }
  }

  private static void filter(Sheet sheet, int count, int endColumn) {
    sheet.createFreezePane(0, 1);
    sheet.setAutoFilter(new CellRangeAddress(0, count, 0, endColumn));
    sheet.setRepeatingRows(new CellRangeAddress(0, 0, -1, -1));
  }

  private void headers(Sheet sheet, int row, int start, String... labels) {
    for (int i = 0; i < labels.length; i++) cell(sheet, row, start + i, labels[i]).setCellStyle(header);
    sheet.getRow(row).setHeightInPoints(26);
  }

  private void row(Sheet sheet, int row, Object... values) {
    for (int i = 0; i < values.length; i++) cell(sheet, row, i, values[i]);
  }

  private Cell cell(Sheet sheet, int row, int column, Object value) {
    var target = sheet.getRow(row);
    if (target == null) target = sheet.createRow(row);
    var cell = target.createCell(column);
    var styles = rowStyles[row % rowStyles.length];
    cell.setCellStyle(styles[0]);
    if (value instanceof LocalDate day) {
      cell.setCellValue(day);
      cell.setCellStyle(styles[2]);
    } else if (value instanceof Number amount) {
      cell.setCellValue(amount.doubleValue());
      cell.setCellStyle(styles[1]);
    } else if (value != null) {
      cell.setCellValue(value.toString());
    }
    return cell;
  }
}
