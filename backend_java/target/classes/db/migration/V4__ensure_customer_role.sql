MERGE INTO CLOTHING_SHOP.ROLES target
USING (SELECT 'CUSTOMER' AS ROLE_CODE FROM DUAL) source
   ON (target.ROLE_CODE = source.ROLE_CODE)
 WHEN MATCHED THEN
      UPDATE SET target.ROLE_NAME = N'Khách hàng',
                 target.DESCRIPTION = N'Người dùng mua hàng trên website',
                 target.DELETE_FLAG = 0
 WHEN NOT MATCHED THEN
      INSERT (ROLE_CODE, ROLE_NAME, DESCRIPTION, DELETE_FLAG)
      VALUES ('CUSTOMER', N'Khách hàng', N'Người dùng mua hàng trên website', 0);
