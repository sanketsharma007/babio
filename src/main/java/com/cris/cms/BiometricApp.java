package com.cris.cms;
import java.sql.*;

import SecuGen.FDxSDKPro.jni.*;

public class BiometricApp {
    private JSGFPLib sgfplib = null;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/babio";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Babio@123";
    
    // Database connection helper
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    // Store fingerprint template in database
    public boolean storeImage(byte[] minutiaeBuffer, String crewid, String fingerval, String rereg, String serialno) {
        System.out.println(">>>>>>>>>>> In Stored1");
        
        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO FP_Data(crewid_v, finger_v, fingerprint_B, Device_Name_v, synched) VALUES(?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            pstmt.setString(1, crewid);
            pstmt.setString(2, fingerval);
            pstmt.setBytes(3, minutiaeBuffer);
            pstmt.setString(4, "SecugenHU20");
            pstmt.setString(5, rereg);
            
            int result = pstmt.executeUpdate();
            System.out.println("Database insertion result: " + result);
            return result > 0;
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return false;
        }
    }
    
    // Get stored template from database
    private byte[] getStoredTemplate(String crewid, String finger) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT fingerprint_B FROM FP_Data WHERE crewid_v=? AND finger_v=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, crewid);
            pstmt.setString(2, finger);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getBytes("fingerprint_B");
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        return null;
    }
    
    // Fingerprint registration
    public boolean fingerRegistration(String crewid, String finger, int timeout, String rereg) {
        long err;
        SGDeviceInfoParam deviceInfo = new SGDeviceInfoParam();
        SGFingerInfo fingerInfo = new SGFingerInfo();
        
        try {
            System.out.println("1. INSTANTIATION");
            // 1. Create SGFPMDev object
            sgfplib = new JSGFPLib();
            
            System.out.println("2. INITIALIZING");
            // 2. Initialize
            err = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
            if (err != SGFDxErrorCode.SGFDX_ERROR_NONE) {
                System.err.println("INITIALIZATION ERROR = " + err);
                return false;
            }
            
            System.out.println("3. OPENING");
            // 3. Open Device
            err = sgfplib.OpenDevice(0);
            if (err != SGFDxErrorCode.SGFDX_ERROR_NONE) {
                System.err.println("OPEN ERROR = " + err);
                return false;
            }
            
            if (err == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                System.out.println("4. BRIGHTNESS");
                // 4. Set brightness
                sgfplib.SetBrightness(30);
                
                System.out.println("5. GET DEVICE INFO");
                // 5. Get device info
                deviceInfo.deviceID = 0;
                err = sgfplib.GetDeviceInfo(deviceInfo);
                
                String serialno = new String(deviceInfo.deviceSN()).trim();
                System.out.println("Device Serial: " + serialno);
                System.out.println("Image Width: " + deviceInfo.imageWidth);
                System.out.println("Image Height: " + deviceInfo.imageHeight);
                
                System.out.println("6. CAPTURE FINGERPRINT");
                sgfplib.SetLedOn(true);
                System.out.println("Capture 1. Please place [" + finger + "] on sensor and press <ENTER>");
                
                byte[] imageBuffer = new byte[(int)(deviceInfo.imageWidth * deviceInfo.imageHeight)];
                err = sgfplib.GetImage(imageBuffer);
                
                if (err == SGFDxErrorCode.SGFDX_ERROR_TIME_OUT) {
                    System.out.println("TIMEOUT");
                    cleanup();
                    return false;
                }
                
                // 7. Check image quality
                int[] quality = new int[1];
                err = sgfplib.GetImageQuality(deviceInfo.imageWidth, deviceInfo.imageHeight, imageBuffer, quality);
                
                System.out.println("Quality: " + quality[0]);
                if (quality[0] < 30) {
                    System.out.println("POOR QUALITY");
                    cleanup();
                    return false;
                }
                
                // 8. Set template format to ANSI378
                System.out.println("8. SET TEMPLATE FORMAT");
                err = sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ANSI378);
                
                // 9. Get max template size
                int[] templateSizeMax = new int[1];
                err = sgfplib.GetMaxTemplateSize(templateSizeMax);
                System.out.println("Max Template Size: " + templateSizeMax[0]);
                
                // 10. Create template
                System.out.println("10. CREATE TEMPLATE");
                byte[] ansiMinutiaeBuffer = new byte[(int)templateSizeMax[0]];
                fingerInfo.FingerNumber = SGFingerPosition.SG_FINGPOS_UK;
                fingerInfo.ViewNumber = 1;
                fingerInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP;
                fingerInfo.ImageQuality = (int)quality[0];
                
                err = sgfplib.CreateTemplate(fingerInfo, imageBuffer, ansiMinutiaeBuffer);
                
                if (err == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                    int[] templateSize = new int[1];
                    err = sgfplib.GetTemplateSize(ansiMinutiaeBuffer, templateSize);
                    System.out.println("Template Size: " + templateSize[0]);
                    
                    // 11. Store template in database
                    byte[] actualTemplate = new byte[(int)templateSize[0]];
                    System.arraycopy(ansiMinutiaeBuffer, 0, actualTemplate, 0, (int)templateSize[0]);
                    
                    boolean stored = storeImage(actualTemplate, crewid, finger, rereg, serialno);
                    
                    sgfplib.SetLedOn(false);
                    cleanup();
                    
                    if (stored) {
                        System.out.println("Registration Complete");
                        return true;
                    } else {
                        System.out.println("Database storage failed");
                        return false;
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error during registration: " + e.getMessage());
            e.printStackTrace();
        }
        
        cleanup();
        return false;
    }
    
    // Fingerprint verification
    public boolean verify(String crewid, String finger, int attempt, int timeout) {
        long err;
        SGDeviceInfoParam deviceInfo = new SGDeviceInfoParam();
        SGFingerInfo fingerInfo = new SGFingerInfo();
        boolean matched = false;
        
        try {
            System.out.println("1. INSTANTIATION");
            // Initialize device (similar to registration)
            sgfplib = new JSGFPLib();
            err = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
            if (err != SGFDxErrorCode.SGFDX_ERROR_NONE) {
                System.err.println("INITIALIZATION ERROR = " + err);
                return false;
            }
            
            err = sgfplib.OpenDevice(0);
            if (err != SGFDxErrorCode.SGFDX_ERROR_NONE) {
                System.err.println("OPEN ERROR = " + err);
                return false;
            }
            
            if (err == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                sgfplib.SetBrightness(30);
                deviceInfo.deviceID = 0;
                sgfplib.GetDeviceInfo(deviceInfo);
                
                // Capture current fingerprint
                sgfplib.SetLedOn(true);
                System.out.println("Verification attempt " + attempt + ". Please place [" + finger + "] on sensor");
                
                byte[] imageBuffer = new byte[(int)(deviceInfo.imageWidth * deviceInfo.imageHeight)];
                err = sgfplib.GetImage(imageBuffer);
                
                if (err == SGFDxErrorCode.SGFDX_ERROR_TIME_OUT) {
                    System.out.println("TIMEOUT");
                    cleanup();
                    return false;
                }
                
                // Check quality and create template
                int[] quality = new int[1];
                sgfplib.GetImageQuality(deviceInfo.imageWidth, deviceInfo.imageHeight, imageBuffer, quality);
                
                System.out.println("Quality: " + quality[0]);
                if (quality[0] < 30) {
                    System.out.println("POOR QUALITY");
                    cleanup();
                    return false;
                }
                
                // Create current template
                sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_ANSI378);
                int[] templateSizeMax = new int[1];
                sgfplib.GetMaxTemplateSize(templateSizeMax);
                
                byte[] currentTemplate = new byte[(int)templateSizeMax[0]];
                fingerInfo.FingerNumber = SGFingerPosition.SG_FINGPOS_UK;
                fingerInfo.ViewNumber = 1;
                fingerInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP;
                fingerInfo.ImageQuality = (int)quality[0];
                
                sgfplib.CreateTemplate(fingerInfo, imageBuffer, currentTemplate);
                
                // Get stored template from database
                byte[] storedTemplate = getStoredTemplate(crewid, finger);
                if (storedTemplate == null) {
                    System.out.println("No stored template found for " + crewid + " finger " + finger);
                    cleanup();
                    return false;
                }
                
                // Match templates
                int[] score = new int[1];
                err = sgfplib.GetMatchingScore(currentTemplate, storedTemplate, score);
                
                System.out.println("Score: " + score[0]);
                if (score[0] > 80) {
                    System.out.println("<<MATCH>>");
                    matched = true;
                } else {
                    System.out.println("<<NO MATCH>>");
                }
                
                sgfplib.SetLedOn(false);
                cleanup();
            }
            
        } catch (Exception e) {
            System.err.println("Error during verification: " + e.getMessage());
            e.printStackTrace();
        }
        
        return matched;
    }
    
    // Cleanup resources
    private void cleanup() {
        if (sgfplib != null) {
            try {
                sgfplib.CloseDevice();
                sgfplib = null;
            } catch (Exception e) {
                System.err.println("Cleanup error: " + e.getMessage());
            }
        }
    }
}