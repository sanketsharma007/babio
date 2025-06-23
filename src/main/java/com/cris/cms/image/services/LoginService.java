package com.cris.cms.image.services;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Vector;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.RestTemplate;

import com.cris.cms.image.model.LoginForm;
import com.cris.cms.image.utility.DBConnection;

@Service
public class LoginService {

    public String showCrewDetails(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "crewdetails";
    }

    public String turnOn(@ModelAttribute LoginForm loginForm, Model model) {
        System.out.println("Inside Turn On");

        String ret = "";
        ProcessBuilder probuilder = new ProcessBuilder("./ba", "DUMMY", "10");

        try {
            probuilder.directory(new File("/usr/local"));
            Process process = probuilder.start();

            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                ret += line + "\n";
            }
            int exitValue = process.waitFor();

            loginForm.setOutput(ret);

            System.out.println("\n\nExit Value is " + exitValue);

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return "success";
    }

    public String initiateBA(@ModelAttribute LoginForm loginForm, Model model) {
        // Handle BreathAnalyzer initiation logic
        System.out.println(">>>>>>>>>>>>>>Inside Initiate BA>>>>>>>>>>>");
        System.out.println("Delete the previous BA Image");
        deleteImage();

        System.out.println("<<<<<<<<<<<<<<Inside Initiate BA<<<<<<<<<<<<");

        return "welcome";
    }

    public ResponseEntity<String> startBreath(@ModelAttribute LoginForm loginForm, Model model) throws Exception {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  startBreath   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        StringBuilder sb = new StringBuilder();
        String crewid = loginForm.getCrewid().trim();
        String crewstatus = loginForm.getCrewstatus();
        String signonid = loginForm.getSignonid();
        String ba_device = "";
        DBConnection db = new DBConnection();

        try {
            ResultSet rs = db.executeQuery(
                    "SELECT * FROM Devices_Enable A,Device_Details B WHERE A.Device_Type_v='BA' AND B.Device_Type_v='BA' AND Selected_v='Y'");
            if (rs.next()) {
                loginForm.setTimeout(rs.getString("Timeout_value_v").trim());
                ba_device = rs.getString("Device_Name_v").trim();
            } else {
                loginForm.setTimeout("10"); // DEFAULT TIMEOUT 10 SEC
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Timeout Exception " + e);
        } finally {
            db.closeCon();
        }

        String camstatus = "N";
        try {
            ResultSet rs = db.executeQuery("SELECT * FROM Devices_Enable WHERE Device_Type_v='CAM'");
            if (rs.next()) {
                camstatus = rs.getString("Enable_v").trim();
            }
            loginForm.setCamstatus(camstatus);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Timeout Exception " + e);
        } finally {
            db.closeCon();
        }

        try {
            killProcess("ba");
            ProcessBuilder pb;
            if (loginForm.getBarepeat().equals("true")) {
                pb = new ProcessBuilder("./ba", "2", loginForm.getTimeout());
            } else {
                pb = new ProcessBuilder("./ba", "1", loginForm.getTimeout());
            }
            pb.directory(new File("/usr/local"));
            Process p = pb.start();

            String line = "";
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            boolean clicked = false;

            if (camstatus.equals("Y")) {
                if (ba_device.equalsIgnoreCase("QTEL")) {
                    while (!(line.contains("&@")) && (line = r.readLine()) != null) {
                        sb.append(line).append("\n");
                        System.out.println("Line : " + line);

                        try {
                            if (!clicked && line.contains("Blowing")) {
                                click(crewid);
                                clicked = true;
                            }
                            if (line.contains("Blow Failure")) {
                                clicked = false;
                                deleteImage();
                            }
                        } catch (NullPointerException e) {
                            System.out.println("CAMERA ERROR: " + e);
                            sb.append("CAMERA ERROR\n");
                            break;
                        }
                    }
                } else {
                    while (!(line.contains("Exhale time")) && (line = r.readLine()) != null) {
                        sb.append(line).append("\n");

                        try {
                            if (!clicked && line.contains("Blowing")) {
                                click(crewid);
                                clicked = true;
                            }
                            if (line.contains("Blow Failure")) {
                                clicked = false;
                                deleteImage();
                            }
                        } catch (NullPointerException e) {
                            System.out.println("CAMERA ERROR: " + e);
                            sb.append("CAMERA ERROR\n");
                            break;
                        }
                    }
                }
            } else {
                if (ba_device.equalsIgnoreCase("KY8000_CMS")) {
                    while (!(line.contains("calon")) && (line = r.readLine()) != null) {
                        sb.append(line).append("\n");

                    }
                } else if (ba_device.equalsIgnoreCase("QTEL")) {
                    while (!(line.contains("&@")) && (line = r.readLine()) != null) {
                        sb.append(line).append("\n");

                    }
                } else {
                    while (!(line.contains("Exhale time")) && (line = r.readLine()) != null) {
                        sb.append(line).append("\n");

                    }
                }
            }

            sb.append(line).append("\n");
            if (camstatus.equals("Y")) {
                try {
                    compress(crewid);
                    File file = new File("/var/www/" + crewid + ".jpeg");
                    FileInputStream imageInFile;
                    if (file.exists()) {
                        imageInFile = new FileInputStream(file);
                        byte[] imageData = new byte[(int) file.length()];
                        imageInFile.read(imageData);
                        String imageDataString = Base64.encodeBase64URLSafeString(imageData);
                        sb.append(imageDataString).append("\n");
                        imageInFile.close();
                    }
                    System.out.println("Image Successfully Manipulated!");
                } catch (FileNotFoundException e) {
                    System.out.println("Image not found" + e);
                } catch (IOException ioe) {
                    System.out.println("Exception while reading the Image " + ioe);
                }
            }
            sb.append(":image64ends:").append("\n");

            r.close();
        } catch (Exception e) {
            System.out.println("Ex : " + e);
        }
        return ResponseEntity.ok(sb.toString());
    }

    private void click(String crewid) {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  Click   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "streamer -f jpeg -o /var/www/" + crewid
                    + "_temp.jpeg && chmod 777 /var/www/" + crewid + "_temp.jpeg");
            pb.directory(new File("/var/www"));
            pb.start();
        } catch (Exception e) {
            System.out.println("Camera Ex : " + e);
        }
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  Click   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }

    private void compress(String crewid) throws FileNotFoundException, IOException {
        try {
            Thread.sleep(2000);
            String filename = "/var/www/" + crewid;
            File imageFile = new File(filename + "_temp.jpeg");
            File compressedImageFile = new File(filename + ".jpeg");
            InputStream inputStream = new FileInputStream(imageFile);
            OutputStream outputStream = new FileOutputStream(compressedImageFile);
            float imageQuality = 0.1f;
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName("jpeg");
            if (!imageWriters.hasNext())
                throw new IllegalStateException("Writers Not Found!!");
            ImageWriter imageWriter = imageWriters.next();
            ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
            imageWriter.setOutput(imageOutputStream);
            ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();
            imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            imageWriteParam.setCompressionQuality(imageQuality);
            imageWriter.write(null, new IIOImage(bufferedImage, null, null), imageWriteParam);
            inputStream.close();
            outputStream.close();
            imageOutputStream.close();
            imageWriter.dispose();
            ProcessBuilder pb1 = new ProcessBuilder("/bin/sh", "-c", "chmod 777 /var/www/" + crewid + ".jpeg");
            pb1.directory(new File("/var/www"));
            pb1.start();
        } catch (Exception ex) {
            System.out.println("Compress Issue" + ex);
            ex.printStackTrace();
        }
    }

    private void killProcess(String process) {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  killProcess   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        ProcessBuilder pb = null;
        Process pr = null;
        try {
            Vector<String> commands = new Vector<>();
            commands.add("pidof");
            commands.add(process);
            pb = new ProcessBuilder(commands);
            pr = pb.start();
            pr.waitFor();
            if (pr.exitValue() == 0) {
                BufferedReader outReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                for (String pid : outReader.readLine().trim().split(" ")) {
                    Runtime.getRuntime().exec("kill " + pid).waitFor();
                }
                outReader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pr != null) {
                pr.destroy();
            }
        }
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  killProcess   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }

    private boolean deleteImage() {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "rm -f *.jpeg");
            pb.directory(new File("/var/www"));
            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            r.close();
        } catch (Exception e) {
            System.out.println("Ex : " + e);
            return false;
        }
        return true;
    }

    public String initiateBio(@ModelAttribute LoginForm loginForm, Model model) {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  initiateBio   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        DBConnection db = new DBConnection();
        String crewid = loginForm.getCrewid().trim();
        System.out.println("Crew ID : " + crewid);
        System.out.println("Reregistration : " + loginForm.getReregistration());

        ResultSet rs = null;
        String viewName = "bioVer"; // default view

        try {
            rs = db.executeQuery("SELECT * FROM FP_Data WHERE crewid_v='" + crewid + "'");
            if (rs.next()) { // IF FP EXISTS IN LOCAL DB
                rs.last();
                int count = rs.getRow();

                if (count == 2) { // IF THERE ARE EXACTLY 2 RECORDS
                    rs.first();
                    System.out.println("Verification");
                    String finger = rs.getString("finger_v");
                    System.out.println("First Finger : " + finger);
                    loginForm.setFirst_finger(finger);

                    rs.next();
                    finger = rs.getString("finger_v");
                    System.out.println("Second Finger : " + finger);
                    loginForm.setSecond_finger(finger);

                    viewName = "bioReg";
                } else { // DATA IS INCONSISTENT - DELETE EXISTING DATA AND GO FOR FRESH REGISTRATION
                    db.executeUpdate("DELETE FROM FP_Data WHERE crewid_v='" + crewid + "'");
                    System.out.println("Registration 1");
                    viewName = "bioReg";
                }
            } else if (!"true".equals(loginForm.getReregistration())) { // IF FP DOESN'T EXIST IN LOCAL DB, CHECK IN
                                                                        // CENTRAL DB
                ResultSet rsPeers = db.executeQuery("SELECT peer_ip_v FROM peers");
                if (rsPeers.next()) { // IF CENTRAL SERVER IP IS CONFIGURED
                    try {
                        String output = getFPData(rsPeers.getString("peer_ip_v"), crewid);
                        if ("[null]".equals(output)) { // IF FP NOT FOUND ON CENTRAL SERVER
                            System.out.println("WS returned null");
                            viewName = "bioReg";
                        } else { // IF FP FOUND ON CENTRAL SERVER
                            // parseJSONOutput(output, crewid);
                            // loginForm.setFirst_finger(finger_no[0]);
                            // loginForm.setSecond_finger(finger_no[1]);
                            viewName = "bioVer";
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("could not connect to remote");
                        viewName = "bioReg";
                    }
                } else { // IF CENTRAL SERVER IP IS NOT CONFIGURED
                    System.out.println("Central Server not configured");
                    viewName = "bioReg";
                }
            } else { // IF IT IS A CASE OF RE-REGISTRATION
                System.out.println("Re - Registration ");
                viewName = "bioReg";
            }
        } catch (Exception e) {
            System.out.println("Error : " + e);
        } finally {
            try {
                if (rs != null)
                    rs.close();
                db.closeCon();
            } catch (Exception ex) {
                // ignore
                ex.printStackTrace();
            }
        }

        // Add the loginForm to the model so the view can use its data
        model.addAttribute("loginForm", loginForm);

        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  initiateBio   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("\n\n\n\n\n");

        return viewName; // returns "bioReg" or "bioVer"
    }

    public String getFPData(String webserviceIp, String crewid) {
        String output = "";
        try {
            System.out.println("Connecting to WS");

            String myurl = "http://" + webserviceIp + "/cmsfpws/getfpdata/'" + crewid + "'";
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.getForEntity(myurl, String.class);

            if (response.getStatusCode().value() != 200) {
                return "failed";
            }

            output = response.getBody();
            System.out.println("Server Output : " + output);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("could not connect to remote");
        }

        return output;
    }

    public ResponseEntity<String> bioVer(@ModelAttribute LoginForm loginForm, Model model) throws Exception {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  BioVer   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        ResultSet rs = null;
        String crewid = loginForm.getCrewid().trim();
        DBConnection db = new DBConnection();

        System.out.println("Crew ID : " + crewid);
        String first_finger = loginForm.getFirst_finger().trim();
        System.out.println("First Finger : " + first_finger);

        String second_finger = loginForm.getSecond_finger().trim();
        System.out.println("Second Finger : " + second_finger);

        StringBuilder sb = new StringBuilder();

        try {
            rs = db.executeQuery("SELECT * FROM Devices_Enable WHERE Device_Type_v='BIO'");
            if (rs.next())
                loginForm.setTimeout(rs.getString("Timeout_value_v").trim() + "000");
            else
                loginForm.setTimeout("10000"); // DEFAULT TIMEOUT 10 SEC
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Timeout Exception " + e);
        } finally {
            db.closeCon();
        }

        try {
            killProcess("bio");
            ProcessBuilder pb = new ProcessBuilder("./bio", "V", crewid, first_finger, second_finger,
                    loginForm.getTimeout());
            pb.directory(new File("/usr/local"));

            Process p = pb.start();

            String line;
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");

                if (line.contains("<<NO MATCH>>")) { // IF VERIFICATION FAILS, DOWNLOAD THE LATEST FINGER PRINTS
                    rs = db.executeQuery("SELECT peer_ip_v FROM peers");

                    if (rs.next()) { // IF CENTRAL SERVER IP IS CONFIGURED IN THIS THIN CLIENT
                        try {
                            String output = getFPData(rs.getString("peer_ip_v"), crewid);

                            if (!output.equals("[null]")) { // IF FP FOUND ON CENTRAL SERVER
                                // parseJSONOutput(output, crewid);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("could not connect to remote");
                        }
                    }
                }
                System.out.println("Line : " + line);
            }

            r.close();

        } catch (Exception e) {
            System.out.println("Ex : " + e);
        }

        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  BioVer   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("\n\n\n\n\n");
        return ResponseEntity.ok(sb.toString());
    }

    public ResponseEntity<String> bioReg(@ModelAttribute LoginForm loginForm, Model model) throws Exception {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  BioReg   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        StringBuilder sb = new StringBuilder();
        DBConnection db = new DBConnection();

        String crewid = loginForm.getCrewid().trim();
        System.out.println("Crew ID : " + crewid);
        String finger = loginForm.getFinger().trim();
        System.out.println("Finger : " + finger);
        String reregistration = loginForm.getReregistration();
        System.out.println("reregistration : " + reregistration);

        try {
            ResultSet rs = db.executeQuery("SELECT * FROM Devices_Enable WHERE Device_Type_v='BIO'");
            if (rs.next())
                loginForm.setTimeout(rs.getString("Timeout_value_v").trim() + "000");
            else
                loginForm.setTimeout("10000"); // DEFAULT TIMEOUT 10 SEC
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Timeout Exception " + e);
        } finally {
            db.closeCon();
        }

        try {
            killProcess("bio");
            String dummy_value = "N"; // USED FOR DIFFERENCIATING THE RE-REGISTRATION CASE

            if ("true".equals(reregistration)) {
                dummy_value = "R"; // SPECIFIES THE RE-REGISTRATION CASE
            }

            ProcessBuilder pb = new ProcessBuilder("./bio", "R", crewid, finger, dummy_value, loginForm.getTimeout());
            pb.directory(new File("/usr/local"));

            Process p = pb.start();

            String line;
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));

            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
                System.out.println("Line : " + line);
            }

            r.close();

        } catch (Exception e) {
            System.out.println("Ex : " + e);
            sb.append("ERROR: ").append(e.getMessage());
        }

        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  BioReg   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        System.out.println("\n\n\n\n\n");

        return ResponseEntity.ok(sb.toString());
    }

   public ResponseEntity<String> deleteFPData(@ModelAttribute LoginForm loginForm) {
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  deleteFPData   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

    DBConnection db = new DBConnection();
    String crewid = loginForm.getCrewid().trim();
    loginForm.setReregistration("true");

    System.out.println("Crew ID : " + crewid);

    int rowcount = 0;
    String result = "fail";

    try {
        rowcount = db.executeUpdate("DELETE FROM FP_Data WHERE crewid_v='" + crewid + "'");
        if (rowcount > 0)
            result = "success";
    } catch (Exception e) {
        System.out.println("Error : " + e);
    } finally {
        db.closeCon();
    }

    System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<  deleteFPData   <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    System.out.println("\n\n\n\n\n");

    return ResponseEntity.ok(result);
}

}
