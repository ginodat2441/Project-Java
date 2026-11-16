WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
-- Preserve all existing product codes and SKUs. Sequence allocation is concurrency-safe;
-- gaps after rollback are intentional and codes are never reused.
DECLARE
  next_number NUMBER;
BEGIN
  SELECT NVL(MAX(CASE WHEN REGEXP_LIKE(PRODUCT_CODE,'^SP[0-9]+$')
                     THEN TO_NUMBER(SUBSTR(PRODUCT_CODE,3)) END),0)+1
    INTO next_number FROM PRODUCT;
  EXECUTE IMMEDIATE 'CREATE SEQUENCE SHOP_PRODUCT_CODE_SEQ START WITH ' ||
    TO_CHAR(next_number,'TM9','NLS_NUMERIC_CHARACTERS=''.,''') || ' INCREMENT BY 1 NOCACHE NOCYCLE';
END;
/
INSERT INTO SHOP_MIGRATION(VERSION) VALUES ('V003');
COMMIT;
