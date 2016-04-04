package eu.aetherc0r3.presfhmmy.armageddon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView bpm, temp, uvi;
    private BluetoothSocket bt_socket;
    private OutputStream out_stream;
    private InputStream in_stream;
    boolean kill_worker = false;
    Handler handler = new Handler();
    private Vibrator vib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bpm = (TextView) findViewById(R.id.bpm);
        bpm.setText("N/A");
        temp = (TextView) findViewById(R.id.temp);
        temp.setText("N/A");
        uvi = (TextView) findViewById(R.id.uvi);
        uvi.setText("N/A");

        BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bt_device = bt_adapter.getRemoteDevice("98:D3:31:20:60:CB");

        bt_adapter.cancelDiscovery();
        try {
            bt_socket = bt_device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            bt_socket.connect();
            out_stream = bt_socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        startListeningThread();
        startTimer();
    }

    public void startListeningThread() {
        try {
            in_stream = bt_socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !kill_worker) {
                    try {
                        //Read the incoming response
                        while (in_stream.available() > 0) {
                            final char command = (char) in_stream.read();
                            char next;
                            String value = "";
                            next = (char) in_stream.read();
                            while (next != command) {
                                value += next;
                                next = (char) in_stream.read();
                            }

                            //Send command back to the main thread
                            final String finalValue = value;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    switch (command) {
                                        case 'p':
                                            update_bpm(finalValue);
                                            break;
                                        case 't':
                                            update_temp(finalValue);
                                            break;
                                        case 'u':
                                            update_uv(finalValue);
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {
                        kill_worker = true;
                    }
                }
            }
        });

        worker.start();
    }

    public void update_bpm(String b) {
        bpm.setText(b);
    }

    public void update_temp(String t) {
        temp.setText(t);
        if (Float.parseFloat(t) >= 40) {
            temp.setTextColor(0xffff0000);
            temp.append("\n Seek medical treatment!");
            vib.vibrate(2000);
        } else if (Float.parseFloat(t) >= 38) {
            temp.setTextColor(0xffff8000);
            temp.append("\n You are getting really hot...");
            vib.vibrate(1000);
        } else if (Float.parseFloat(t) >= 37) {
            temp.setTextColor(0xffffff00);
            temp.append("\n Your temperature is rising...");
        } else if (Float.parseFloat(t) < 35) {
            temp.setTextColor(0xff00bfff);
            temp.append("\n Your temperature is lower than normal.");
            vib.vibrate(1000);
        } else {
            temp.setTextColor(0xff000000);
        }
    }

    public void update_uv(String i) {
        uvi.setText(i);
        if (Float.parseFloat(i) < 3) {
            uvi.setTextColor(0xff00ff00);
        } else if (Float.parseFloat(i) < 6) {
            uvi.setTextColor(0xffffff00);
        } else if (Float.parseFloat(i) < 8) {
            uvi.setTextColor(0xffff8000);
            uvi.append("\n You should consider wearing sunscreen.");
        }  else if (Float.parseFloat(i) < 11){
            uvi.setTextColor(0xffff0000);
            uvi.append("\n Wear sunscreen!!!");
            vib.vibrate(1000);
        } else {
            uvi.setTextColor(0xff8000ff);
            uvi.append("\n Avoid being outside...");
            vib.vibrate(2000);

        }

    }

    private void startTimer() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                get_bpm();
            }
        }, 0, 1000);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                get_temp();
            }
        }, 0, 30000);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                get_UVIndex();
            }
        }, 0, 60000);
    }

    public void get_bpm() {
        try {
            out_stream.write('p');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void get_temp() {
        try {
            out_stream.write('t');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void get_UVIndex() {
        try {
            out_stream.write('u');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            bt_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
