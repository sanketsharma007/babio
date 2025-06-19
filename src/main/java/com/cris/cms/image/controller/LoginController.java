package com.cris.cms.image.controller;

import com.cris.cms.image.model.LoginForm;
import com.cris.cms.image.utility.DBConnection;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage ;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.Vector;

@Controller
@RequestMapping("/babio")
public class LoginController {

    @GetMapping
    public String showCrewDetails(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "crewdetails";
    }
    
    @PostMapping("/turnOn")
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

    @PostMapping("/initiateBA")
    public String initiateBA(@ModelAttribute LoginForm loginForm, Model model) {
        // Handle BreathAnalyzer initiation logic
        System.out.println(">>>>>>>>>>>>>>Inside Initiate BA>>>>>>>>>>>");
        System.out.println("Delete the previous BA Image");
        //deleteImage();

        System.out.println("<<<<<<<<<<<<<<Inside Initiate BA<<<<<<<<<<<<");

        return "welcome";
    }

    @PostMapping("/startBreath")
    public void startBreath(@ModelAttribute LoginForm loginForm, HttpServletResponse response) throws Exception {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  startBreath   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

        PrintWriter out = response.getWriter();
        String crewid = loginForm.getCrewid().trim();
        String crewstatus = loginForm.getCrewstatus();
        String signonid = loginForm.getSignonid();
        String ba_device = "";
        DBConnection db = new DBConnection();

        try {
            ResultSet rs = db.executeQuery("SELECT * FROM Devices_Enable A,Device_Details B WHERE A.Device_Type_v='BA' AND B.Device_Type_v='BA' AND Selected_v='Y'");
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

            String line="";
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            boolean clicked = false;

            if (camstatus.equals("Y")) {
                if (ba_device.equalsIgnoreCase("QTEL")) {
                    while (!(line.contains("&@")) && (line = r.readLine()) != null) {
                        out.println(line);
                        out.flush();
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
                            out.println("CAMERA ERROR");
                            out.flush();
                            break;
                        }
                    }
                } else {
                    while (!(line.contains("Exhale time")) && (line = r.readLine()) != null) {
                        out.println(line);
                        out.flush();
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
                            out.println("CAMERA ERROR");
                            out.flush();
                            break;
                        }
                    }
                }
            } else {
                if (ba_device.equalsIgnoreCase("KY8000_CMS")) {
                    while (!(line.contains("calon")) && (line = r.readLine()) != null) {
                        out.println(line);
                        out.flush();
                    }
                } else if (ba_device.equalsIgnoreCase("QTEL")) {
                    while (!(line.contains("&@")) && (line = r.readLine()) != null) {
                        out.println(line);
                        out.flush();
                    }
                } else {
                    while (!(line.contains("Exhale time")) && (line = r.readLine()) != null) {
                        out.println(line);
                        out.flush();
                    }
                }
            }

            out.println(" image64:");
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
                        out.println(imageDataString);
                        imageInFile.close();
                    }
                    System.out.println("Image Successfully Manipulated!");
                } catch (FileNotFoundException e) {
                    System.out.println("Image not found" + e);
                } catch (IOException ioe) {
                    System.out.println("Exception while reading the Image " + ioe);
                }
            }
            out.println(":image64ends:");
            out.flush();
            r.close();
            out.close();
        } catch (Exception e) {
            System.out.println("Ex : " + e);
        }
    }

    private void click(String crewid) {
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  Click   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "streamer -f jpeg -o /var/www/" + crewid + "_temp.jpeg && chmod 777 /var/www/" + crewid + "_temp.jpeg");
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
            if (!imageWriters.hasNext()) throw new IllegalStateException("Writers Not Found!!");
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
}
