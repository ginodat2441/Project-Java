package com.example.clothingshop.admin;

import com.example.clothingshop.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import static com.example.clothingshop.admin.AdminRequests.*;
import static com.example.clothingshop.admin.AdminRepository.number;

@Service
public class ProductFormService {
    private final AdminService admin;
    private final ImageService images;
    private final AdminRepository db;
    public ProductFormService(AdminService admin,ImageService images,AdminRepository db){this.admin=admin;this.images=images;this.db=db;}

    @Transactional(rollbackFor=Exception.class)
    public long save(Long id,Product r,List<MultipartFile> files,List<ImageInput> metadata,long actor)throws IOException{
        if(files.isEmpty()||files.size()>20||files.size()!=metadata.size())throw new BusinessException("Danh sách ảnh không hợp lệ (tối đa 20 ảnh)");
        if(metadata.stream().filter(ImageInput::cover).count()>1)throw new BusinessException("Chỉ chọn một ảnh đại diện");
        if(id==null&&r.status()==1&&metadata.stream().noneMatch(ImageInput::cover))throw new BusinessException("Chọn ảnh đại diện trước khi bật bán");
        var draft=new Product(r.code(),r.name(),r.brandId(),r.materialId(),r.categoryId(),r.gender(),r.description(),r.thumbnailUrl(),0,r.variants(),r.brandName(),r.materialName());
        long product=admin.product(id,draft,actor);
        for(int i=0;i<files.size();i++){
            var m=metadata.get(i);if(m.order()<0)throw new BusinessException("Thứ tự ảnh không được âm");
            long image=images.upload(product,m.colorId(),files.get(i),actor);
            images.edit(product,image,m.colorId(),m.order(),m.cover(),actor);
        }
        var saved=db.one("SELECT THUMBNAIL_URL,(SELECT COUNT(*) FROM PRODUCT_DETAIL WHERE PRODUCT_ID=:i AND STATUS=1 AND DELETE_FLAG=0) SELLING FROM PRODUCT WHERE ID=:i","i",product);
        if(r.status()==1&&(saved.get("THUMBNAIL_URL")==null||number(saved,"SELLING")==0))throw new BusinessException("Sản phẩm đang bán cần ảnh đại diện và biến thể đang bán");
        db.update("UPDATE PRODUCT SET STATUS=:s WHERE ID=:i","s",r.status(),"i",product);
        return product;
    }
}
