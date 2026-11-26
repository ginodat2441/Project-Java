package com.example.clothingshop.common;

import com.fasterxml.jackson.databind.JsonMappingException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.validation.FieldError;

/** Converts validation and known Oracle errors into labels shown on forms. */
public final class FormErrors {
  private static final Pattern COLUMN = Pattern.compile("\"([A-Z][A-Z0-9_$#]*)\"\\s*\\)");
  private static final Pattern CONSTRAINT = Pattern.compile("\\((?:\"?[A-Z0-9_$#]+\"?\\.)?\"?([A-Z][A-Z0-9_$#]*)\"?\\)");

  private FormErrors() {}

  public static String label(String field) {
    String name = field.replaceAll("\\[\\d*\\]", "").replaceFirst("^.*\\.", "");
    return switch (name.replace("_", "").toLowerCase(Locale.ROOT)) {
      case "username", "loginname" -> "Tên đăng nhập";
      case "email" -> "Email";
      case "password", "newpassword", "passwordhash" -> "Mật khẩu";
      case "confirmpassword", "passwordconfirmation" -> "Xác nhận mật khẩu";
      case "fullname" -> "Họ và tên";
      case "identifier" -> "Tên đăng nhập hoặc email";
      case "phone", "phonenumber" -> "Số điện thoại";
      case "receiverphone" -> "Số điện thoại người nhận";
      case "receivername" -> "Tên người nhận";
      case "address", "addressid" -> "Địa chỉ";
      case "detailaddress" -> "Địa chỉ chi tiết";
      case "province", "provincecode" -> "Tỉnh/Thành phố";
      case "district", "districtcode" -> "Quận/Huyện";
      case "ward", "wardcode" -> "Phường/Xã";
      case "name" -> "Tên";
      case "brand", "brandname", "brandid" -> "Thương hiệu";
      case "material", "materialname", "materialid" -> "Chất liệu";
      case "color", "colorname", "colorid" -> "Màu sắc";
      case "hex", "hexcode" -> "Mã màu";
      case "size" -> "Kích cỡ";
      case "roleid", "managerroleid" -> "Vai trò";
      case "status" -> "Trạng thái";
      case "gender", "targetgender" -> "Giới tính";
      case "code" -> "Mã";
      case "vouchercode" -> "Mã giảm giá";
      case "productcode" -> "Mã sản phẩm";
      case "productname" -> "Tên sản phẩm";
      case "sku" -> "SKU";
      case "items", "productids", "productid" -> "Sản phẩm";
      case "variantid", "productdetailid" -> "Biến thể";
      case "openingunitcost" -> "Đơn giá vốn đầu kỳ";
      case "categoryid" -> "Danh mục";
      case "parentid" -> "Danh mục cha";
      case "customerid" -> "Khách hàng";
      case "quantity", "delta" -> "Số lượng";
      case "expectedquantity" -> "Số lượng dự kiến";
      case "actualquantity" -> "Số lượng thực tế";
      case "price" -> "Giá bán";
      case "unitcost" -> "Đơn giá vốn";
      case "paymentmethod" -> "Phương thức thanh toán";
      case "paymentstatus", "paid" -> "Trạng thái thanh toán";
      case "shippingservice" -> "Dịch vụ vận chuyển";
      case "expectedshippingfee", "shippingfee" -> "Phí vận chuyển";
      case "carrier" -> "Đơn vị vận chuyển";
      case "trackingcode" -> "Mã vận đơn";
      case "thumbnailurl", "images", "image" -> "Ảnh";
      case "startsat" -> "Ngày bắt đầu";
      case "endsat" -> "Ngày kết thúc";
      case "usagelimit" -> "Giới hạn lượt dùng";
      case "value", "discountvalue" -> "Giá trị giảm";
      case "maxdiscount" -> "Giảm tối đa";
      case "minorder" -> "Đơn tối thiểu";
      case "type", "discounttype" -> "Loại";
      case "lines" -> "Dòng hàng";
      case "reason" -> "Lý do";
      case "description" -> "Mô tả";
      case "note", "conditionnote" -> "Ghi chú";
      case "token" -> "Mã xác nhận";
      case "bootstraptoken" -> "Mã khởi tạo";
      case "evidence" -> "Chứng từ";
      case "order" -> "Thứ tự";
      default -> null;
    };
  }

  public static String validation(FieldError error) {
    String label = label(error.getField());
    String object = error.getObjectName().toLowerCase(Locale.ROOT);
    String field = error.getField();
    if (field.equals("name")) {
      if (object.contains("category")) label = "Tên danh mục";
      else if (object.contains("product")) label = "Tên sản phẩm";
      else if (object.contains("brand")) label = "Tên thương hiệu";
      else if (object.contains("color")) label = "Tên màu";
    }
    if (object.contains("voucher")) {
      if (field.equals("code")) label = "Mã giảm giá";
      if (field.equals("type")) label = "Loại giảm";
      if (field.equals("value")) label = "Mức giảm";
      if (field.equals("startsAt")) label = "Có hiệu lực từ";
      if (field.equals("endsAt")) label = "Hết hạn";
      if (field.equals("minOrder")) label = "Đơn tối thiểu trên sản phẩm áp dụng";
      if (field.equals("usageLimit")) label = "Giới hạn lượt";
      if (field.startsWith("productIds")) label = "Sản phẩm được áp dụng";
    }
    String code = error.getCode() == null ? "" : error.getCode();
    Object[] args = error.getArguments();
    String reason = switch (code) {
      case "NotBlank", "NotNull", "NotEmpty" -> "là trường bắt buộc.";
      case "Email" -> "không đúng định dạng.";
      case "Min", "DecimalMin" -> "phải từ " + argument(args, 1) + " trở lên.";
      case "Max", "DecimalMax" -> "không được vượt quá " + argument(args, 1) + ".";
      case "Size" -> sizeMessage(field, args);
      case "Positive" -> "phải lớn hơn 0.";
      case "PositiveOrZero" -> "phải từ 0 trở lên.";
      case "Digits" -> "vượt quá số chữ số cho phép.";
      case "Pattern" -> switch (field) {
        case "username" -> "chỉ được dùng 3–100 chữ cái không dấu, chữ số, dấu chấm, gạch dưới hoặc gạch ngang.";
        case "phone", "receiverPhone" -> "không đúng định dạng số điện thoại.";
        case "hex", "hexCode" -> "phải theo dạng #RRGGBB, ví dụ #FF0000.";
        case "code" -> "chỉ được dùng chữ cái không dấu, chữ số, dấu gạch dưới hoặc gạch ngang.";
        default -> "không đúng định dạng hoặc nằm ngoài các lựa chọn cho phép.";
      };
      default -> "có giá trị không hợp lệ.";
    };
    Matcher row = Pattern.compile("^(?:items|lines|variants|images)\\[(\\d+)\\]\\.").matcher(field);
    String position = row.find() ? " ở dòng " + (Integer.parseInt(row.group(1)) + 1) : "";
    return (label == null ? "Trường nhập liệu" : label) + position + " " + reason;
  }

  private static String sizeMessage(String field, Object[] args) {
    Object max = argument(args, 1);
    Object min = argument(args, 2);
    String unit = Set.of("items", "lines", "productIds", "images", "variants").contains(field)
        ? " mục" : " ký tự";
    return Integer.valueOf(0).equals(min) ? "tối đa " + max + unit + "."
        : "phải có từ " + min + " đến " + max + unit + ".";
  }

  private static Object argument(Object[] args, int index) {
    return args != null && args.length > index ? args[index] : "";
  }

  public static String invalidValue(String field) {
    String label = label(field);
    return label == null ? "Dữ liệu gửi lên không hợp lệ." : label + " có giá trị không hợp lệ.";
  }

  public static String unreadable(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
        var path = mapping.getPath();
        for (int index = path.size() - 1; index >= 0; index--) {
          String field = path.get(index).getFieldName();
          if (field != null && label(field) != null) return invalidValue(field);
        }
      }
    }
    return "Dữ liệu gửi lên thiếu hoặc sai định dạng.";
  }

  public static String database(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (!(cause instanceof SQLException sql) || sql.getMessage() == null) continue;
      String detail = sql.getMessage().toUpperCase(Locale.ROOT);
      if (sql.getErrorCode() == 1400 || sql.getErrorCode() == 1407 || sql.getErrorCode() == 12899) {
        Matcher columns = COLUMN.matcher(detail);
        if (columns.find()) {
          String label = label(columns.group(1));
          if (label != null) return label + (sql.getErrorCode() == 12899
              ? " vượt quá độ dài cho phép." : " là trường bắt buộc.");
        }
      } else if (sql.getErrorCode() == 1 || sql.getErrorCode() == 2290 || sql.getErrorCode() == 2291) {
        Matcher constraint = CONSTRAINT.matcher(detail);
        if (!constraint.find()) continue;
        String label = switch (constraint.group(1)) {
          case "UQ_USERS_USERNAME", "UQ_MANAGER_USERNAME" -> "Tên đăng nhập";
          case "UQ_USERS_EMAIL", "UQ_USERS_EMAIL_ACTIVE", "UQ_MANAGER_EMAIL" -> "Email";
          case "UQ_PRODUCT_CODE" -> "Mã sản phẩm";
          case "UQ_PRODUCT_DETAIL_SKU" -> "SKU";
          case "CK_VOUCHER_DATES" -> "Hết hạn";
          case "CK_VOUCHER_PERCENT" -> "Mức giảm";
          case "FK_PRODUCT_BRAND" -> "Thương hiệu";
          case "FK_PRODUCT_MATERIAL" -> "Chất liệu";
          case "FK_PD_COLOR" -> "Màu sắc";
          case "FK_BILL_ADDRESS" -> "Địa chỉ";
          case "FK_MANAGER_ROLE", "FK_USERS_ROLE" -> "Vai trò";
          case "CK_PD_PRICE" -> "Giá bán";
          case "CK_PD_QUANTITY", "CK_CD_QTY", "CK_BD_QTY" -> "Số lượng";
          case "CK_BILL_PAY_METHOD" -> "Phương thức thanh toán";
          case "CK_BILL_PAY_STATUS" -> "Trạng thái thanh toán";
          default -> null;
        };
        if (label == null) continue;
        if ("CK_VOUCHER_DATES".equals(constraint.group(1))) return label + " phải sau ngày bắt đầu.";
        if ("CK_VOUCHER_PERCENT".equals(constraint.group(1))) return label + " không được vượt quá 100%.";
        return label + switch (sql.getErrorCode()) {
          case 1 -> " đã được sử dụng.";
          case 2291 -> " đã chọn không còn tồn tại.";
          default -> " có giá trị không hợp lệ.";
        };
      }
    }
    return null;
  }
}
