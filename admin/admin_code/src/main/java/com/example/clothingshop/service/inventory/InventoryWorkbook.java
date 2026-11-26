package com.example.clothingshop.service.inventory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

final class InventoryWorkbook {
  private static final String[] KEYS = {
    "SKU", "PRODUCT_NAME", "COLOR_NAME", "SIZE_NAME",
    "PHYSICAL_OPENING", "RESERVED_OPENING", "OPENING",
    "RECEIPT", "RETURNED", "DISPATCH", "ISSUE", "RESERVED", "RELEASED", "ADJUSTMENT",
    "PHYSICAL_CLOSING", "RESERVED_CLOSING", "CLOSING", "LEGACY_ROWS"
  };
  private static final String[] HEADERS = {
    "SKU", "Sản phẩm", "Màu", "Size", "Tại kho", "Đang giữ", "Sẵn bán",
    "Nhập kho", "Hoàn nhập", "Xuất giao", "Xuất khác", "Giữ mới", "Giải giữ", "Kiểm kê / đảo",
    "Tại kho", "Đang giữ", "Sẵn bán", "Dòng cũ"
  };

  @SuppressWarnings("unchecked")
  static byte[] write(Map<String, Object> report, String query) throws IOException {
    var items = (List<Map<String, Object>>) report.get("items");
    var totals = (Map<String, Object>) report.get("totals");
    var dates = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    String period = "Kỳ báo cáo  " + ((LocalDate) report.get("from")).format(dates)
        + " – " + ((LocalDate) report.get("to")).format(dates);
    try (var book = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
      var title = style(book, IndexedColors.DARK_BLUE, IndexedColors.WHITE, true, 16);
      var header = style(book, IndexedColors.DARK_TEAL, IndexedColors.WHITE, true, 11);
      header.setWrapText(true);
      var emphasis = style(book, IndexedColors.PALE_BLUE, IndexedColors.DARK_BLUE, true, 11);
      var plain = style(book, IndexedColors.WHITE, IndexedColors.DARK_BLUE, false, 11);
      var alternate = style(book, IndexedColors.GREY_25_PERCENT, IndexedColors.DARK_BLUE, false, 11);
      short numberFormat = book.createDataFormat().getFormat("#,##0;[Red](#,##0);0");
      for (var style : List.of(plain, alternate, emphasis)) style.setDataFormat(numberFormat);

      var summary = book.createSheet("TỔNG QUAN");
      layout(summary, 35, 19, 5, 34, 19);
      banner(summary, 0, 0, 4, "BÁO CÁO NHẬP XUẤT TỒN", title);
      banner(summary, 1, 0, 4, period, plain);
      banner(summary, 2, 0, 4, query.isBlank() ? "Phạm vi: tất cả SKU" : "Bộ lọc: " + query, plain);
      banner(summary, 4, 0, 1, "Tồn sẵn bán", header);
      banner(summary, 4, 3, 4, "Phát sinh trong kỳ", header);
      metric(summary, 5, 0, "Sẵn bán đầu kỳ", totals.get("OPENING"), plain);
      metric(summary, 6, 0, "Sẵn bán cuối kỳ", totals.get("CLOSING"), emphasis);
      metric(summary, 7, 0, "Số SKU", items.size(), plain);
      metric(summary, 8, 0, "Dòng cũ cần đối chiếu", totals.get("LEGACY_ROWS"), plain);
      for (int i = 7; i <= 13; i++) {
        metric(summary, i - 2, 3, HEADERS[i], totals.get(KEYS[i]), i % 2 == 1 ? plain : alternate);
      }
      banner(summary, 13, 0, 4, "Sẵn bán = tại kho − đang giữ. Các số liệu đều tính theo số lượng.", plain);
      banner(summary, 14, 0, 4, "Ô trống ở tồn tại kho hoặc đang giữ: cần đối chiếu lịch sử cũ.", plain);
      if (items.isEmpty()) banner(summary, 16, 0, 4, "Không có SKU phù hợp với bộ lọc.", emphasis);

      var detail = book.createSheet("CHI TIẾT SKU");
      layout(detail, 22, 34, 15, 9, 12, 12, 12, 11, 12, 11, 11, 11, 11, 12, 12, 12, 12, 11);
      banner(detail, 0, 0, 17, "CHI TIẾT NHẬP XUẤT TỒN THEO SKU", title);
      banner(detail, 1, 0, 17, period, plain);
      banner(detail, 2, 0, 3, "Sản phẩm", header);
      banner(detail, 2, 4, 6, "Tồn đầu kỳ", header);
      banner(detail, 2, 7, 13, "Phát sinh trong kỳ", header);
      banner(detail, 2, 14, 16, "Tồn cuối kỳ", header);
      value(detail, 2, 17, "Đối chiếu", header);
      detail.getRow(2).setHeightInPoints(22);
      for (int col = 0; col < HEADERS.length; col++) value(detail, 3, col, HEADERS[col], header);
      detail.getRow(3).setHeightInPoints(28);
      for (int i = 0; i < items.size(); i++) {
        var item = items.get(i);
        var band = i % 2 == 0 ? plain : alternate;
        for (int col = 0; col < KEYS.length; col++) {
          value(detail, i + 4, col, item.get(KEYS[col]), col == 16 ? emphasis : band);
        }
      }
      detail.createFreezePane(2, 4);
      detail.setAutoFilter(new CellRangeAddress(3, items.size() + 3, 0, KEYS.length - 1));
      detail.setRepeatingRows(CellRangeAddress.valueOf("3:4"));
      book.write(output);
      return output.toByteArray();
    }
  }

  private static CellStyle style(Workbook book, IndexedColors fill, IndexedColors ink, boolean bold, int size) {
    var style = book.createCellStyle();
    style.setFillForegroundColor(fill.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setVerticalAlignment(VerticalAlignment.CENTER);
    var font = book.createFont();
    font.setFontName("Calibri");
    font.setColor(ink.getIndex());
    font.setBold(bold);
    font.setFontHeightInPoints((short) size);
    style.setFont(font);
    return style;
  }

  private static void layout(Sheet sheet, int... widths) {
    sheet.setDisplayGridlines(false);
    sheet.setDefaultRowHeightInPoints(20);
    for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
    sheet.setFitToPage(true);
    var print = sheet.getPrintSetup();
    print.setLandscape(true);
    print.setFitWidth((short) 1);
    print.setFitHeight((short) 0);
  }

  private static void banner(Sheet sheet, int row, int first, int last, String text, CellStyle style) {
    sheet.addMergedRegion(new CellRangeAddress(row, row, first, last));
    for (int col = first; col <= last; col++) {
      value(sheet, row, col, col == first ? text : null, style);
    }
    if (row == 0) sheet.getRow(row).setHeightInPoints(32);
  }

  private static void metric(Sheet sheet, int row, int col, String label, Object amount, CellStyle style) {
    value(sheet, row, col, label, style);
    value(sheet, row, col + 1, amount == null ? 0 : amount, style);
  }

  private static void value(Sheet sheet, int row, int col, Object value, CellStyle style) {
    var target = sheet.getRow(row);
    if (target == null) target = sheet.createRow(row);
    var cell = target.createCell(col);
    cell.setCellStyle(style);
    if (value instanceof Number number) cell.setCellValue(number.doubleValue());
    else if (value != null) cell.setCellValue(value.toString());
  }
}
