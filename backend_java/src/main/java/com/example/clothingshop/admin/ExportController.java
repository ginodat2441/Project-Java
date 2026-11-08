package com.example.clothingshop.admin;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.time.*;
import java.util.*;
@RestController
public class ExportController {
 private final ReportService reports;private final AdminSearchService search;private final InventoryReportService inventory;
 public ExportController(ReportService r,AdminSearchService s,InventoryReportService i){reports=r;search=s;inventory=i;}
 LocalDate date(Map<String,String> f,String k){String v=f.get(k);return v==null||v.isBlank()?null:LocalDate.parse(v);}
 Integer integer(Map<String,String> f,String k){String v=f.get(k);return v==null||v.isBlank()?null:Integer.valueOf(v);}
 @SuppressWarnings("unchecked") @GetMapping("/api/admin/reports/export") public ResponseEntity<String> reports(@RequestParam Map<String,String> f){
  Integer year=integer(f,"year");Long product=f.get("productId")==null?null:Long.valueOf(f.get("productId"));String detail=f.getOrDefault("detail","period");var rows=new ArrayList<Map<String,Object>>();
  for(int page=1;;page++){var d=reports.report(f.getOrDefault("period","MONTH"),year==null?LocalDate.now().getYear():year,integer(f,"month"),integer(f,"quarter"),date(f,"from"),date(f,"to"),product,f.getOrDefault("q",""),page);
   if(detail.equals("sku")){rows.addAll((List<Map<String,Object>>)d.get("variants"));break;}if(detail.equals("period")){rows.addAll((List<Map<String,Object>>)d.get("series"));break;}if(!detail.equals("product"))throw new IllegalArgumentException("Chọn period, product hoặc sku");
   var products=(Map<String,Object>)d.get("products");var items=(List<Map<String,Object>>)products.get("items");rows.addAll(items);if(items.isEmpty()||rows.size()>=((Number)products.get("total")).longValue())break;
  }return Csv.response(rows,"doanh-thu-"+detail);
 }
 @SuppressWarnings("unchecked") @GetMapping("/api/admin/inventory/export") public ResponseEntity<String> inventory(@RequestParam Map<String,String> f){
  var rows=new ArrayList<Map<String,Object>>();String detail=f.getOrDefault("detail","balance");
  for(int page=1;;page++){Map<String,Object> d;if(detail.equals("history")){var filters=new HashMap<>(f);filters.put("page",Integer.toString(page));d=(Map<String,Object>)search.history(filters);}else d=inventory.report(date(f,"from"),date(f,"to"),f.getOrDefault("q",""),page);
   var items=(List<Map<String,Object>>)d.get("items");rows.addAll(items);if(items.isEmpty()||rows.size()>=((Number)d.get("total")).longValue())break;
  }return Csv.response(rows,"ton-kho-"+detail);
 }
}
