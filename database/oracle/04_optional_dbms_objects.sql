/* Oracle DBMS objects demo cho phần function/procedure/trigger. */

CREATE OR REPLACE FUNCTION FN_QR_RISK_LEVEL (
    p_scan_count_today NUMBER,
    p_sale_status VARCHAR2,
    p_qr_status VARCHAR2,
    p_freshness_status VARCHAR2
) RETURN VARCHAR2
IS
BEGIN
    IF p_sale_status = 'SOLD' OR p_qr_status IN ('REVOKED','EXPIRED') OR p_freshness_status = 'EXPIRED' THEN
        RETURN 'DANGER';
    ELSIF p_scan_count_today > 40 OR p_qr_status = 'SUSPICIOUS' THEN
        RETURN 'WARNING';
    ELSE
        RETURN 'SAFE';
    END IF;
END;
/

CREATE OR REPLACE PROCEDURE PRC_LOG_QR_SCAN (
    p_qr_token IN VARCHAR2,
    p_ip_address IN VARCHAR2,
    p_user_agent IN VARCHAR2
)
IS
    v_product_id PRODUCTS.PRODUCT_ID%TYPE;
    v_qr_status QR_CODES.STATUS%TYPE;
    v_sale_status QR_CODES.SALE_STATUS%TYPE;
    v_freshness VARCHAR2(30);
    v_count NUMBER;
    v_level VARCHAR2(20);
BEGIN
    SELECT q.PRODUCT_ID, q.STATUS, q.SALE_STATUS, NVL(v.FRESHNESS_STATUS, 'UNKNOWN')
    INTO v_product_id, v_qr_status, v_sale_status, v_freshness
    FROM QR_CODES q LEFT JOIN VW_PUBLIC_TRACE v ON q.PRODUCT_ID=v.PRODUCT_ID
    WHERE q.QR_TOKEN = p_qr_token;

    SELECT COUNT(*) INTO v_count
    FROM QR_SCAN_LOGS
    WHERE QR_TOKEN = p_qr_token AND TRUNC(CAST(SCANNED_AT AS DATE)) = TRUNC(CURRENT_DATE);

    v_level := FN_QR_RISK_LEVEL(v_count + 1, v_sale_status, v_qr_status, v_freshness);

    INSERT INTO QR_SCAN_LOGS(QR_TOKEN, PRODUCT_ID, IP_ADDRESS, USER_AGENT, RESULT_STATUS, WARNING_LEVEL, WARNING_MESSAGE)
    VALUES(p_qr_token, v_product_id, p_ip_address, p_user_agent, 'SUCCESS',
           CASE v_level WHEN 'SAFE' THEN 'NONE' WHEN 'WARNING' THEN 'MEDIUM' ELSE 'HIGH' END,
           'Scan by PRC_LOG_QR_SCAN - risk=' || v_level);

    IF v_level <> 'SAFE' THEN
        INSERT INTO QR_SECURITY_ALERTS(QR_TOKEN, PRODUCT_ID, ALERT_TYPE, ALERT_LEVEL, ALERT_MESSAGE)
        VALUES(p_qr_token, v_product_id, 'QR_SCAN_RISK', CASE v_level WHEN 'WARNING' THEN 'MEDIUM' ELSE 'HIGH' END,
               'DB procedure phát hiện QR có rủi ro: ' || v_level);
    END IF;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        INSERT INTO QR_SCAN_LOGS(QR_TOKEN, IP_ADDRESS, USER_AGENT, RESULT_STATUS, WARNING_LEVEL, WARNING_MESSAGE)
        VALUES(p_qr_token, p_ip_address, p_user_agent, 'INVALID_TOKEN', 'CRITICAL', 'QR không thuộc hệ thống.');
END;
/

CREATE OR REPLACE TRIGGER TRG_QR_CODES_SET_SOLD_AT
BEFORE UPDATE OF SALE_STATUS ON QR_CODES
FOR EACH ROW
BEGIN
    IF :NEW.SALE_STATUS = 'SOLD' AND :OLD.SALE_STATUS <> 'SOLD' THEN
        :NEW.SOLD_AT := CURRENT_TIMESTAMP;
    END IF;
    IF :NEW.SALE_STATUS <> 'SOLD' THEN
        :NEW.SOLD_AT := NULL;
    END IF;
END;
/

CREATE OR REPLACE TRIGGER TRG_AUDIT_LOGS_APPEND_ONLY
BEFORE UPDATE OR DELETE ON AUDIT_LOGS
BEGIN
    RAISE_APPLICATION_ERROR(-20001, 'AUDIT_LOGS là bảng append-only, không được UPDATE/DELETE.');
END;
/

CREATE OR REPLACE PROCEDURE PRC_PRINT_OPEN_QR_ALERTS
IS
BEGIN
    FOR r IN (
        SELECT ALERT_ID, PRODUCT_ID, ALERT_TYPE, ALERT_LEVEL, ALERT_MESSAGE, CREATED_AT
        FROM QR_SECURITY_ALERTS
        WHERE RESOLVED_STATUS='OPEN'
        ORDER BY CREATED_AT DESC
    ) LOOP
        DBMS_OUTPUT.PUT_LINE('ALERT #' || r.ALERT_ID || ' | PRODUCT=' || r.PRODUCT_ID || ' | ' || r.ALERT_LEVEL || ' | ' || r.ALERT_MESSAGE);
    END LOOP;
END;
/
