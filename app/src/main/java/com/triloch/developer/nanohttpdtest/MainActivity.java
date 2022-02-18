package com.triloch.developer.nanohttpdtest;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    EditText editText;
    TextView infoIp;
    String msgLog = "";
    ServerSocket httpServerSocket;
    WebView webView;
    Button buttonLoadURL;
    String rootName = "rootname";
    UsbManager usbManager;
    UsbDevice myDevice = null;
    String ipAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText) findViewById(R.id.welcomemsg);
        infoIp = (TextView) findViewById(R.id.infoip);
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setAllowFileAccess(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        buttonLoadURL = findViewById(R.id.buttomLoadURL);
        buttonLoadURL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView.loadUrl(ipAddress);
            }
        });
        infoIp.setText(getIpAddress() + ":" + HttpServerThread.HttpServerPORT + "\n");

        HttpServerThread httpServerThread = new HttpServerThread();
        httpServerThread.start();

    

    }
    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                myDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if(usbManager.hasPermission(myDevice)) {
                    Toast.makeText(MainActivity.this, "USB DEVICE ATTACHED", Toast.LENGTH_LONG).show();
                    UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                    Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                    if(deviceIterator.hasNext()) {
                        myDevice = deviceIterator.next();
                        readDevice();
                    }
                } else {
                    //usbManager.requestPermission(myDevice, permissionIntent);
                }
                //usbManager.requestPermission(myDevice, permissionIntent);
            } else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Toast.makeText(MainActivity.this, "USB DEVICE DETACHED", Toast.LENGTH_LONG).show();
                if(device != null) {
                    if(device == myDevice) {
                        //releaseUsb();
                    }
                }
            }
        }

    };

    public void readDevice() {
        Toast.makeText(getApplicationContext(), "Reading device", Toast.LENGTH_LONG).show();
        UsbMassStorageDevice[] devices = UsbMassStorageDevice.getMassStorageDevices(this);
        for(UsbMassStorageDevice device : devices) {
            try {
                device.init();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Only uses the first partition on the device
            FileSystem currentFs = device.getPartitions().get(0).getFileSystem();

            UsbFile root = currentFs.getRootDirectory();

            rootName = root.getName();




        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(httpServerSocket != null) {
            try {
                httpServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while(enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while(enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if(inetAddress.isSiteLocalAddress()) {
                        ipAddress = inetAddress.getHostAddress() + ":8888";
                        ip += "SiteLocalAddress: " + inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    private class HttpServerThread extends Thread {

        static final int HttpServerPORT = 8888;

        @Override
        public void run() {
            Socket socket = null;
            try {
                httpServerSocket = new ServerSocket(HttpServerPORT);
                while(true) {
                    socket = httpServerSocket.accept();
                    HttpResponseThread httpResponseThread = new HttpResponseThread(socket, editText.getText().toString());
                    httpResponseThread.start();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class HttpResponseThread extends Thread {
        Socket socket;
        String h1;
        HttpResponseThread(Socket socket, String msg) {
            this.socket = socket;
            h1 = msg;
        }
        @Override
        public void run() {
            BufferedReader is;
            PrintWriter os;
            String request;
            try {
                is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                request = is.readLine();
                os = new PrintWriter(socket.getOutputStream(), true);
                String response = "<html><head></head>" + "<body>" + "<h1>" + h1 + "</h1>" + "</body></html>";

                os.print("HTTP/1.0 200" + "\r\n");
                os.print("Content type: text/html" + "\r\n");
                os.print("Content length: " + response.length() + "\r\n");
                os.print("\r\n");
                os.print(response + "\r\n");
                os.flush();
                socket.close();
                msgLog += "Request of " + request + " from " + socket.getInetAddress().toString() + "\n";
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }
    }

}
