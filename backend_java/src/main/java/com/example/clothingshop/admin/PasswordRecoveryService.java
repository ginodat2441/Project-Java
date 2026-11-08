package com.example.clothingshop.admin;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import com.example.clothingshop.common.BusinessException;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.ConcurrentHashMap;
import static com.example.clothingshop.admin.AdminRepository.number;

@Service
public class PasswordRecoveryService {
 private final AdminRepository db;private final PasswordEncoder encoder;private final RecoveryDelivery delivery;private final String base;
 private final ConcurrentHashMap<String,long[]> attempts=new ConcurrentHashMap<>();private final SecureRandom random=new SecureRandom();
 public PasswordRecoveryService(AdminRepository db,PasswordEncoder encoder,RecoveryDelivery delivery,@Value("${shop.reset.base-url:http://localhost:8080}") String base){this.db=db;this.encoder=encoder;this.delivery=delivery;this.base=base;}
 @Transactional public boolean request(String identifier,String ip){
  long now=System.currentTimeMillis();if(attempts.size()>10000)attempts.entrySet().removeIf(e->e.getValue()[1]<now);
  if(attempts.size()>10000&&!attempts.containsKey(ip))return false;
  long[] attempt=attempts.compute(ip,(k,v)->v==null||v[1]<now?new long[]{1,now+600000}:new long[]{v[0]+1,v[1]});if(attempt[0]>5)return false;
  var found=db.rows("SELECT m.ID,m.EMAIL,m.AUTH_VERSION FROM MANAGER m JOIN MANAGER_ROLE r ON r.ID=m.MANAGER_ROLE_ID WHERE (LOWER(m.USERNAME)=LOWER(:u) OR LOWER(m.EMAIL)=LOWER(:u)) AND m.STATUS=1 AND m.DELETE_FLAG=0 AND r.DELETE_FLAG=0 AND r.ROLE_CODE IN ('ADMIN','STAFF') ORDER BY m.ID","u",identifier.strip());
  if(found.isEmpty())return true;
  var manager=found.get(0);long id=number(manager,"ID");var locked=db.one("SELECT AUTH_VERSION,STATUS FROM MANAGER WHERE ID=:i FOR UPDATE","i",id);if(number(locked,"STATUS")!=1)return true;
  db.update("UPDATE MANAGER_PASSWORD_RESET SET USED_AT=LOCALTIMESTAMP WHERE MANAGER_ID=:i AND USED_AT IS NULL","i",id);
  byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  db.insert("INSERT INTO MANAGER_PASSWORD_RESET(MANAGER_ID,TOKEN_HASH,AUTH_VERSION,EXPIRES_AT) VALUES(:i,:t,:v,LOCALTIMESTAMP+INTERVAL '15' MINUTE)","i",id,"t",hash(token),"v",number(locked,"AUTH_VERSION"));db.audit(id,"PASSWORD_RESET_REQUEST","MANAGER",id);
  String url=base.replaceAll("/+$","")+"/reset-password#token="+token,email=manager.get("EMAIL").toString();
  TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){try{delivery.deliver(email,url);}catch(Exception e){org.slf4j.LoggerFactory.getLogger(PasswordRecoveryService.class).warn("Không gửi được thư khôi phục; kiểm tra cấu hình gửi thư trên máy chủ.");}}});
  return true;
 }
 @Transactional public void reset(String token,String password){
  if(password.length()<12||password.getBytes(StandardCharsets.UTF_8).length>72)throw new BusinessException("Mật khẩu cần ít nhất 12 ký tự và tối đa 72 byte UTF-8");
  var found=db.rows("SELECT ID,MANAGER_ID FROM MANAGER_PASSWORD_RESET WHERE TOKEN_HASH=:t","t",hash(token));
  if(found.isEmpty())throw new BusinessException("Liên kết không hợp lệ, đã dùng hoặc hết hạn");
  long manager=number(found.get(0),"MANAGER_ID");var account=db.one("SELECT AUTH_VERSION,STATUS,DELETE_FLAG FROM MANAGER WHERE ID=:i FOR UPDATE","i",manager);
  var valid=db.rows("SELECT ID,AUTH_VERSION FROM MANAGER_PASSWORD_RESET WHERE TOKEN_HASH=:t AND USED_AT IS NULL AND EXPIRES_AT>LOCALTIMESTAMP FOR UPDATE","t",hash(token));
  if(valid.isEmpty()||number(account,"STATUS")!=1||number(account,"DELETE_FLAG")!=0||number(account,"AUTH_VERSION")!=number(valid.get(0),"AUTH_VERSION"))throw new BusinessException("Liên kết không hợp lệ, đã dùng hoặc hết hạn");
  db.update("UPDATE MANAGER SET PASSWORD_HASH=:p,AUTH_VERSION=AUTH_VERSION+1 WHERE ID=:i","p",encoder.encode(password),"i",manager);
  db.update("UPDATE MANAGER_PASSWORD_RESET SET USED_AT=LOCALTIMESTAMP WHERE MANAGER_ID=:i AND USED_AT IS NULL","i",manager);db.audit(manager,"PASSWORD_RESET","MANAGER",manager);
 }
 private String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
