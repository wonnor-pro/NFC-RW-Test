# NFC Module Testing Record

## Background

Android devices with NFC activated are able to constantly search for available NFC devices when the screen is unlocked. As long as an available NFC device is found, it will parse the NDEF record stored on that device and according to its intent to select the appropriate application to handle the intent.

NDEF data is encapsulated inside a message (NdefMessage) that contains one or more records (NdefRecord). In a well-formed NDEF message, the first NdefRecord contains the following fields:

1. 3-bit TNF (Type Name Format)
2. Variable Length
3.  TypeVariable Length ID
4. Variable Length Payload

The objectives for NFC module required in this Android application is to perform the following tasks:

1. Initialise a new tag and assign a TagID to it;
2. Read the TagID from registered NFC tag;
3. Optional: wipeout/overwrite TagID.

More specifically, the message read from the NFC module should be in a format/intent that is able to trigger our application without popping out the Activity Chooser.

## Test Application

To design the NFC module, a NFC testing Android application is built. This application takes the simplest layout with few interactive features to simulate functions required. Basic operations include reading and writing the tags.

Here several key points and problems encountered (with solutions) are listed for information.

### AndroidManifest.xml

In the AndroidManifest.xml, claims regarding the usage of NFC and intents should be made.

1. Require the usage of NFC

   ```xml
   <uses-permission android:name="android.permission.NFC" />
   ```

2. Catch specific format of records so that no other activity will be called

   We attempt to use URL schemes to identify our own records. An example in this testing application is used in the form of `cnr://tag.id/XXXXXX`, where X represents TagID digits. The intent filter should be set as

   ```xml
   <intent-filter>
       <action android:name="android.nfc.action.NDEF_DISCOVERED"/>
       <category android:name="android.intent.category.DEFAULT"/>
       <data android:scheme="cnr" android:host="tag.id" />
   </intent-filter>
   ```


3. Avoid popping many windows

   As Android has four types of activity launch mode, to avoid popping new windows for new intent, we declare the launch mode to be singleTask.

   ```xml
   <activity android:name=".MainActivity" android:launchMode="singleTask" >
   ```

4. Identify new tags

   New tags are without the specific URL scheme, hence they cannot be caught by this application. The unsolved NDEF message should also be processed by our application, i.e. our application should appear in the Activity Chooser for unsolved NDEF message. Intent filter for nfc.action.TECH_DISCOVERED is added.

   ```xml
   <intent-filter>
       <action android:name="android.nfc.action.TECH_DISCOVERED" />
       </intent-filter>
       <meta-data android:name="android.nfc.action.TECH_DISCOVERED"
           android:resource="@xml/nfc_filter" />
   ```

   With the meta-data file (nfc_filter.xml) saved in the directory of ./res/xml to specify the types of nfc technology that should be caught.

   ```xml
   <resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2" >
     <tech-list>   <tech>android.nfc.tech.NfcA</tech>  </tech-list>
     <tech-list>   <tech>android.nfc.tech.NfcB</tech>  </tech-list>
     <tech-list>   <tech>android.nfc.tech.NfcF</tech>  </tech-list>
     <tech-list>   <tech>android.nfc.tech.NfcV</tech>  </tech-list>
     <tech-list>   <tech>android.nfc.tech.NfcBarcode</tech>  </tech-list>
   </resources>
   ```

### activity_main.xml - layout of the page

<div align=center>
  <img width=300 src="https://tech.connorx.wang/images/nfc/screenshot_01.png" >
  <img width=300 src="https://tech.connorx.wang/images/nfc/screenshot_02.png" >
</div>


The interactive features includes:

*TextView*

- `textTitle` (showing NFC Test)
- `textID` (showing Tag ID:)
- `textStatus` (showing the writing status)
- `textDisplay` (showing ID of the tag)

*EditText*

- `textInput` (taking the intended ID for writing)

*Button*

- `readButton` (to get ID from the tag)
- `clearButton` (to clear all the displayed status)

*Switch*

- `writeSwitch` (swipe when writing mode is on)

### MainActivity.java

The read and write operations are performed by functions of `readTag()` and `writeTag(String load)`.

The `readTag()`function should be able to read the NDEF message from the tag and update the TagID. Key points are as followed (no structures, omit many lines in between):

```java
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;

// For registered tags (triggered by NDEF_DISCOVERED)
if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())){
    tag = (Tag) getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
} 
// For unknown tags (triggered by TAG_DISCOVERED)
else if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())){
    textDisplay.setText("It is a new Tag, please give it an ID");
}

// get the tag object from the intent
tag = (Tag) getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);

// get the message from from the NDEF
Ndef ndef = Ndef.get(tag);
msg = ndef.getCachedNdefMessage();

// extract the TagID from the URL (from the character index 14 to the end)
String payload = new String(record.getPayload());
String actualData = payload.substring(14);
```

The `writeTag(String load)` function should be able to write the URL formatted in variable load to the NFC tag. Depending on the tag's format, the writing process is different. Key points are as followed (no structures, omit many lines in between):

```java
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;

// For registered tags (triggered by NDEF_DISCOVERED)
if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())){
    tag = (Tag) getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
    
    // With NDEF already
    Ndef ndef = Ndef.get(tag);
    ndef.connect();
} 
// For unknown tags (triggered by TAG_DISCOVERED)
else if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(getIntent().getAction())){
    tag = (Tag) getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
    
    // Without NDEF format
    NdefFormatable format = NdefFormatable.get(tag);
    format.connect();
}

// generate the RTD_URL (type: TNF_WELL_KNOWN) 
recordNdefRecord rtdUriRecord1 = NdefRecord.createUri(load);

// write the record to the deviceNdefMessage 
ndefMessage = new NdefMessage(rtdUriRecord1);
ndef.writeNdefMessage(ndefMessage);
// or format.format(ndefMessage)
```

The main structure/logic of the application operation, however, is adjusted during the development. Some problems occurred are put down below with solutions:

1. Automatically read and update the ID when tag is found

   By adding the `readTag()` function to `onNewIntent()`, the application is able to read and update the ID as soon as the application is woken by a NFC tag.

2. Write the ID to the known tag (previously defined in the URL scheme)

   Writing is a bit different from reading as the device goes into onNewIntent everytime it gets an intent for this app, hence it never gets the chance to write to the tag. So if not using pause/resume function and `ForegroundDispatch`, a scheme should be put forward to tackle this.

   One solution is found by setting a status variable isWrite to indicate the writing mode. When the app goes into `onNewIntent()`, if it is in writing mode, the function calls `writeTag(String load)` first. The writing mode should be turned off when the writing task is done.

   In this way, the writing problem is solved.

3. Write the ID to the unregistered tag (empty tags)

   In the testing devices, there are more than one Android application available for the unsolved NDEF message, hence the `Activity Chooser` will pop out and interpret the writing. We attempt to use the ForegroundDispatch.
   
   ```java
   // Define NFC adapter and Pending intent to catch the intents
   private NfcAdapter mNfcAdapter;
   private PendingIntent mPendingIntent;
   
   public void onCreate(Bundle savedInstanceState) {
       ...
       nfcAdapter = NfcAdapter.getDefaultAdapter(this);
       ...
       mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
       mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()), 0);
   }
   
   public void onNewIntent(Intent intent){
       super.onNewIntent(intent);
       setIntent(intent);
       ... // Operations here
   }
   
   public void onResume() {
       super.onResume();
     
       // Enable Foreground Dispatch
       if (mNfcAdapter != null)
           mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
   }
   
   public void onPause() {
       super.onPause();
     
       // Disable Foreground Dispatch
       if (mNfcAdapter != null)
           mNfcAdapter.disableForegroundDispatch(this);
   } 
   ```
   
   

### Integrated Module

Developing...