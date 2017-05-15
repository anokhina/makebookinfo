/*
 * Copyright 2017 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.sevn.makebookinfo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.jcodec.api.awt.FrameGrab;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.json.JSONObject;

public class App {
    public static void main(String args[]) {
        boolean isTest = !true;
        if (!isTest) {
            if (args.length < 2) {
                System.out.println("Usage: java -jar makebookinfo.jar <seek_seconds> <media_file_name>");
                return;
            }
        }
        int seekSecond = 5;
        String mediaFileName = "test\\medium.mp4";
        if (!isTest) {
            seekSecond = new Integer(args[0]);
            mediaFileName = args[1];
        }
        File mediaFile = new File(mediaFileName);
        JSONObject fileProps = new JSONObject();
        boolean canDo = true;
        boolean b = false;
        
        if (canDo) {
            b = mediaFile.exists();
            canDo = canDo && b;
            fileProps.put("exists", b);
        }
        if (canDo) {
            b = mediaFile.canRead();
            canDo = canDo && b;
            fileProps.put("canRead", b);
        }
        if (canDo) {
            b = mediaFile.canWrite();
            canDo = canDo && b;
            fileProps.put("canWrite", b);
        }
        if (canDo) {
            String dirName = mediaFile.getName();
            int idx = dirName.lastIndexOf(".");
            if (idx > 0) {
                dirName = dirName.substring(0, idx);
            } else {
                dirName += "_book";
            }
            File dirFile = new File(mediaFile.getParentFile(), dirName);
            if (dirFile.exists()) {
                System.err.println("Can't manage file:" + mediaFileName + ": exists>" + dirFile.getAbsolutePath());
                return;
            }
            File bookDir = new File(dirFile, "Book");
            File infoDir = new File(dirFile, "Info");
            File imgDir = new File(infoDir, "Image");
            File imgFile = new File(imgDir, "Cover.jpg");
            File bookMediaFile = new File(bookDir, mediaFile.getName());
            bookDir.mkdirs();
            imgDir.mkdirs();
            
            ImageIcon ii = getIIfromVideo(mediaFile, seekSecond, -1);
            if (ii == null) {
                try {
                    ii = getDefaultIcon("/noicon.png");
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (ii != null) {
                BufferedImage bi = getBufferedImage(ii);
                try {
                    writeImage(bi, "png", imgFile);
                    System.out.println("Cover is written");
                } catch (IOException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                Files.copy(mediaFile.toPath(), bookMediaFile.toPath());
                System.out.println("Media file is copied");
            } catch (IOException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } else {
            System.err.println("Can't manage file:" + mediaFileName + ":" + fileProps.toString(2));
        }
    }
    
    private static ImageIcon getDefaultIcon(String name) throws IOException {
        InputStream stream = App.class.getResourceAsStream(name);
        return new ImageIcon(ImageIO.read(stream));            
    }
    
    public static BufferedImage getBufferedImage(ImageIcon icon) {
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig2 = bi.createGraphics();
        ig2.drawImage(icon.getImage(), 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);
        return bi;
    }
    public static void writeImage(BufferedImage bi, String format, File fl) throws IOException {
    	ImageIO.write(bi, format, fl);
    }
    
    public static ImageIcon getIIfromVideo(File mediaFile, int seekSecond, int seekFrame) {
        ImageIcon imageIcon = null;
        try {
            FileChannelWrapper grabch = NIOUtils.readableFileChannel(mediaFile);
            BufferedImage frame = null;
            try {
                FrameGrab grab = new FrameGrab(grabch);
                for (int i = 0; i < 50; i++) {
                    if (seekSecond >= 0) {
                        grab.seekToSecondSloppy(seekSecond);
                    } else {
                        grab.seekToFrameSloppy(seekFrame);
                    }
                    try {
                        frame = grab.getFrame();
                    } catch (Exception e) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            } finally {
                NIOUtils.closeQuietly(grabch);
            }
            if (frame != null) {
                imageIcon = new ImageIcon(frame);
            }
        } catch (Exception ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
        return imageIcon;
    }
}
