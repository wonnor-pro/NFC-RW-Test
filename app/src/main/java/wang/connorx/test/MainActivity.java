package wang.connorx.test;

import androidx.appcompat.app.AppCompatActivity;

import java.net.IDN;
import java.nio.charset.Charset;
import java.util.Locale;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.app.PendingIntent;


import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private Button clearButton;
    private Button readButton;
    private TextView textDisplay;
    private TextView textStatus;
    private EditText textInput;
    private Switch writeSwitch;
    private Tag tag;
    private String load;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;

    private boolean isWrite = false;

    private NfcAdapter nfcAdapter;

    private OnClickListener clearListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            clear();
        }
    };

    private OnClickListener readListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            readTag();
        }
    };


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        readButton = (Button) findViewById(R.id.readButton);
        clearButton = (Button) findViewById(R.id.clearButton);
        textStatus = (TextView) findViewById(R.id.textStatus);
        writeSwitch = (Switch) findViewById(R.id.writeSwitch);
        textDisplay = (TextView) findViewById(R.id.textDisplay);
        textInput = (EditText) findViewById(R.id.textInput);

        clearButton.setOnClickListener(clearListener);
        readButton.setOnClickListener(readListener);
        writeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    isWrite = true;
                    String inputID = textInput.getText().toString();
                    load = "cnr://tag.id/".concat(inputID);
                    textStatus.setText("Ready to write in the ID: ".concat(inputID));
                }else {
                }
            }
        });

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()), 0);

    }


    @Override
    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        if (isWrite){
            writeTag(load);
            textDisplay.setText("Please scan your to check the ID");
            textDisplay.setTextColor(Color.BLACK);
        }
        else{
            readTag();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null,
                    null);

    }

    @Override
    public void onPause() {
        super.onPause();

        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    public void clear() {
        textDisplay.setText("Please tap a tag to scan.");
        textStatus.setText("No task.");
        isWrite = false;
        if (writeSwitch.isChecked()){
            writeSwitch.toggle();
        }
    }

    public void readTag() {
        NdefMessage msg = null;
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())){
            tag = (Tag) getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (tag != null){
                Ndef ndef = Ndef.get(tag);
                msg = ndef.getCachedNdefMessage();
                for (NdefRecord record:msg.getRecords()){
                    if (record != null){
                        String payload = new String(record.getPayload());
                        String actualData = payload.substring(14);
                        textDisplay.setText(actualData);
                        textDisplay.setTextColor(Color.BLACK);
                    }

                    else{
                        Log.i("NFC Test", "No tag detected");
                    } // else
                } // for
            } // if
        } // if
        else if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())){
            textDisplay.setText("It is a new Tag, please give it an ID");
            textDisplay.setTextColor(Color.BLACK);
        }
    }



    public void writeTag(String load){
        textStatus.setText("Ready to write");
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())){
            tag = (Tag)getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null){
                Ndef ndef = Ndef.get(tag);
                try {
                    ndef.connect();
                    NdefRecord rtdUriRecord1 = NdefRecord.createUri(load);
                    NdefMessage ndefMessage = new NdefMessage(rtdUriRecord1);
                    ndef.writeNdefMessage(ndefMessage);
                    writeSwitch.toggle();
                    textStatus.setText("Done!");
                    isWrite = false;
                } catch (Exception e){
                    e.printStackTrace();
                    textStatus.setText("Message cannot be sent, please try again!");
                } finally{
                    try {
                        ndef.close();
                    } catch (Exception e){
                        e.printStackTrace();
                        textStatus.setText("Connection cannot be closed, please try again!");
                    }
                }
            }
        }
        else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())){
            tag = (Tag)getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null){
                NdefFormatable format = NdefFormatable.get(tag);
                try {
                    format.connect();
                    NdefRecord rtdUriRecord1 = NdefRecord.createUri(load);
                    NdefMessage ndefMessage = new NdefMessage(rtdUriRecord1);
                    format.format(ndefMessage);
                    writeSwitch.toggle();
                    textStatus.setText("Done!");
                    isWrite = false;
                } catch (Exception e){
                    e.printStackTrace();
                    textStatus.setText("Message cannot be sent, please try again!");
                } finally{
                    try {
                        format.close();
                    } catch (Exception e){
                        e.printStackTrace();
                        textStatus.setText("Connection cannot be closed, please try again!");
                    }
                }
            }
        }
    }


}
