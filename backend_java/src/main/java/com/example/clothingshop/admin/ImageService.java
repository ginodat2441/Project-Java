package com.example.clothingshop.admin;
import com.example.clothingshop.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import static com.example.clothingshop.admin.AdminRepository.number;

@Service
public class ImageService {
    final AdminRepository db; final Path root;
    public ImageService(AdminRepository db,@Value("${shop.upload-dir}") String root){this.db=db;this.root=Path.of(root).toAbsolutePath().normalize();}
    public Path uploadRoot(){return root;}
    @Transactional public long upload(long product,Long color,MultipartFile file,long actor)throws IOException{
        db.one("SELECT ID FROM PRODUCT WHERE ID=:i AND DELETE_FLAG=0 FOR UPDATE","i",product);
        if(color!=null)db.one("SELECT ID FROM COLOR WHERE ID=:i AND DELETE_FLAG=0","i",color);
        if(file.isEmpty()||file.getSize()>5*1024*1024)throw new BusinessException("Mỗi ảnh phải từ 1 byte đến 5 MB");
        byte[] bytes=file.getBytes();String ext=extension(bytes);
        try(var stream=javax.imageio.ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=javax.imageio.ImageIO.getImageReaders(stream);
            if(!readers.hasNext())throw new BusinessException("Không thể đọc nội dung ảnh");
            var reader=readers.next();try{
                reader.setInput(stream);int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width>12000||height>12000||(long)width*height>40000000)throw new BusinessException("Ảnh quá lớn, tối đa 40 megapixel");
                if(reader.read(0)==null)throw new BusinessException("Ảnh không hợp lệ");
            }finally{reader.dispose();}
        }catch(javax.imageio.IIOException e){throw new BusinessException("Nội dung ảnh bị lỗi");}
        Files.createDirectories(root);String name=UUID.randomUUID()+"."+ext;Path path=root.resolve(name);
        Files.write(path,bytes,StandardOpenOption.CREATE_NEW);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
            @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(path);}catch(IOException ignored){}}
        });
        long id=db.insert("INSERT INTO PRODUCT_IMAGE(PRODUCT_ID,COLOR_ID,IMAGE_URL,SORT_ORDER) VALUES(:p,:c,:u,(SELECT NVL(MAX(SORT_ORDER),0)+1 FROM PRODUCT_IMAGE WHERE PRODUCT_ID=:p))","p",product,"c",color,"u","/uploads/"+name);
        db.audit(actor,"UPLOAD","PRODUCT_IMAGE",id);return id;
    }
    static String extension(byte[] b){
        if(b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)return "jpg";
        if(b.length>=8&&Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "png";
        if(b.length>=20&&new String(b,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(b,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP")&&Set.of("VP8 ","VP8L","VP8X").contains(new String(b,12,4,java.nio.charset.StandardCharsets.US_ASCII))){
            long size=((long)b[4]&255)|(((long)b[5]&255)<<8)|(((long)b[6]&255)<<16)|(((long)b[7]&255)<<24);
            if(size+8==b.length)return "webp";
        }
        throw new BusinessException("Chỉ chấp nhận nội dung JPEG, PNG hoặc WebP hợp lệ");
    }
    @Transactional public void edit(long product,long id,Long color,int order,boolean cover,long actor){
        db.one("SELECT ID FROM PRODUCT WHERE ID=:p FOR UPDATE","p",product);
        var image=db.one("SELECT IMAGE_URL FROM PRODUCT_IMAGE WHERE ID=:i AND PRODUCT_ID=:p","i",id,"p",product);
        if(color!=null)db.one("SELECT ID FROM COLOR WHERE ID=:i AND DELETE_FLAG=0","i",color);
        if(cover){db.update("UPDATE PRODUCT_IMAGE SET IS_COVER=0 WHERE PRODUCT_ID=:p","p",product);db.update("UPDATE PRODUCT SET THUMBNAIL_URL=:u WHERE ID=:p","u",image.get("IMAGE_URL"),"p",product);}
        db.update("UPDATE PRODUCT_IMAGE SET COLOR_ID=:c,SORT_ORDER=:o,IS_COVER=CASE WHEN :cover=1 THEN 1 ELSE IS_COVER END WHERE ID=:i","c",color,"o",order,"cover",cover?1:0,"i",id);
        db.audit(actor,"EDIT","PRODUCT_IMAGE",id);
    }
    @Transactional public void remove(long product,long id,long actor){
        var p=db.one("SELECT STATUS,THUMBNAIL_URL FROM PRODUCT WHERE ID=:p FOR UPDATE","p",product);
        var image=db.one("SELECT IMAGE_URL FROM PRODUCT_IMAGE WHERE ID=:i AND PRODUCT_ID=:p","i",id,"p",product);
        String url=image.get("IMAGE_URL").toString();
        if(url.equals(p.get("THUMBNAIL_URL"))){if(number(p,"STATUS")==1)throw new BusinessException("Chọn ảnh đại diện khác hoặc ẩn sản phẩm trước khi gỡ");db.update("UPDATE PRODUCT SET THUMBNAIL_URL=NULL WHERE ID=:p","p",product);}
        db.update("DELETE FROM PRODUCT_IMAGE WHERE ID=:i","i",id);db.audit(actor,"REMOVE","PRODUCT_IMAGE",id);
        // Retain physical files for safe recovery; cleanup only unreferenced server-generated files.
    }
    public Path path(String name){
        if(!name.matches("[0-9a-f-]{36}\\.(jpg|png|webp)"))throw new BusinessException("Tên ảnh không hợp lệ");
        Path path=root.resolve(name).normalize();if(!path.startsWith(root))throw new BusinessException("Đường dẫn không hợp lệ");return path;
    }
}
