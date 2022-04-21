package com.tevapharm.attte.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class IpAddressUtils {

    public static String getPublicIpAddress() {

        BufferedReader br = null;
        try {
            URL url = new URL("http://checkip.amazonaws.com/");
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            return br.readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return "*";
        }

    }

    public static String getStreamStr(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "GBK"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        br.close();
        return sb.toString();
    }

    public String myIPOctal1() {
        String ip = getPublicIpAddress();

        if (ip.equals("*")) {
            return "*";
        }

        String s1;
        assert ip != null;
        s1 = ip.substring(0, ip.indexOf("."));
        return s1;
    }

    public String myIPOctal2() {
        String ip = getPublicIpAddress();

        if (ip.equals("*")) {
            return "*";
        }

        String s1, s2;
        assert ip != null;
        s1 = ip.substring(0, ip.indexOf("."));
        ip = ip.substring(ip.indexOf(".") + 1);
        s2 = s1 + "." + ip.substring(0, ip.indexOf("."));
        return s2;
    }

    public String myIPOctal3() {
        String ip = getPublicIpAddress();

        if (ip.equals("*")) {
            return "*";
        }

        String s1, s2, s3;
        assert ip != null;
        s1 = ip.substring(0, ip.indexOf("."));
        ip = ip.substring(ip.indexOf(".") + 1);
        s2 = s1 + "." + ip.substring(0, ip.indexOf("."));
        ip = ip.substring(ip.indexOf(".") + 1);
        s3 = s2 + "." + ip.substring(0, ip.indexOf("."));

        return s3;
    }


}
