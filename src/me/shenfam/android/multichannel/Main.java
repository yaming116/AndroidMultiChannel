package me.shenfam.android.multichannel;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    private static final String CHANNEL = "META-INF/cztchannel_%s.txt";
    private List<String> channels;
    private List<File> apks;
    private File srcEmptyFile;
    private File channelConfigPath;


    public static void main(String... args){
        Main main = new Main();
        if (args.length > 0 ){
            main.channelConfigPath = new File(args[0]);
            if (!main.channelConfigPath.exists()){
                throw new RuntimeException("channel config path not exist");
            }
        }else {
            main.channelConfigPath = new File("./");
        }
        long start = System.currentTimeMillis();
        main.initApks();
        main.initChannel();
        main.initEmptyFile();
        main.buildChannelApk();

        System.out.println("total time: "  + (System.currentTimeMillis() - start) / 1000  + "s");
    }

    private void initApks(){
        if (apks == null) {
            apks = new ArrayList<>();
        }else {
            apks.clear();
        }
        File apkDir = new File(channelConfigPath, "info/apks");
        if (!apkDir.exists()){
            throw new IllegalArgumentException("can't find apk files in :"  + apkDir.getAbsolutePath());
        }
        File[] files = apkDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".apk");
            }
        });

        for (File f : files){
            apks.add(f);
        }

        if (apks.isEmpty()){
            throw new RuntimeException("not fond apk in :" + apkDir.getAbsolutePath());
        }

        System.out.println("init apk finish");
    }

    private void initChannel(){
        if (channels == null){
            channels = new ArrayList<>();
        }else {
            channels.clear();
        }

        File channelFile = new File(channelConfigPath, "info/channel.txt");
        if (!channelFile.exists()){
            throw new IllegalArgumentException("can't find channel file at :"  + channelFile.getAbsolutePath());
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(channelFile));
            String ch = null;
            while ((ch = reader.readLine()) != null){
                channels.add(ch);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("read channel file error");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("read channel file error");
        }finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("init channel finish");
    }

    private void initEmptyFile(){
        if (srcEmptyFile == null){
            srcEmptyFile = new File(channelConfigPath, "info/czt.txt");
        }

        if (!srcEmptyFile.exists()) {
            try {
                srcEmptyFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("create czt.txt file eroor");
            }
        }
    }

    private void buildChannelApk(){


        for (File f : apks){
            String fileName = f.getName();
            String filerRealName = fileName.split("\\.")[0];

            File storeDir = new File(f.getParentFile(), filerRealName);
            if (!storeDir.exists()){
                storeDir.mkdirs();
            }

            for (String channel : channels){
                File outChannelFile = new File(storeDir, filerRealName + "_" + channel + ".apk");
                try {
                    FileUtils.copyFile(f, outChannelFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("copy apk file error" + f.getAbsolutePath());
                }
                try {
                    Map<String, String> env = new HashMap<>();
                    env.put("create", "true");
                    // locate file system by using the syntax
                    // defined in java.net.JarURLConnection
                    URI uri = URI.create("jar:" + outChannelFile.toURI());

                    try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
                        Path externalTxtFile = Paths.get(srcEmptyFile.toURI());
                        Path pathInZipfile = zipfs.getPath(String.format(CHANNEL, channel));
                        // copy a file into the zip file
                        Files.copy(externalTxtFile, pathInZipfile,
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("build file error");
                }finally {

                }

                System.out.println("build " + outChannelFile.getName() +  " finish");
            }

            System.out.println("build apk finish");
        }
    }
}
