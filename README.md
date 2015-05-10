A very simple color picker widget. <br />

<b>Installation:</b>

<ul>
  <li>Get the .aar file from <a href="https://github.com/triforce930/ColorPickerViewAndroid/blob/master/kscolorpicker-release.aar?raw=true">here</a>. </li>
  <li>Right click on the Android Studio project and click "Open Module Settings". </li>
  <li>Click the plus button over the module list. </li>
  <li>Select "import .jar or .aar file" from the list. Select the .aar file you have downloaded before. </li>
  <li>On "Dependencies" tab, add the imported module to the list.</li>
</ul>

That should be all.

If gradle build returns with an error, check what the error message suggests you to do, or try adding this line under your manifest's application tag:

<code>tools:replace="android:icon"</code>

<b>Usage:</b>
  <p>
    <i>activity_main.xml</i>
    
      <com.karacasoft.colorpicker.ColorPickerView
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:id="@+id/color_picker_view"
      android:layout_below="@+id/textView"
      android:padding="20dp"
      android:layout_alignParentLeft="true"
      android:layout_alignParentStart="true" />
    
  </p>
  <p>
    <i>MainActivity.java</i>
    
      ...
      
      ColorPickerView colorPicker;
      TextView tv;
      
      @Override
      protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.activity_main);
  
          colorPicker = (ColorPickerView) findViewById(R.id.color_picker_view);
          tv = (TextView) findViewById(R.id.textView);
          colorPicker.setOnColorPickListener(new ColorPickerView.OnColorPickListener() {
              @Override
              public void onColorPick(int color) {
  
              }
          });
  
          colorPicker.setOnPinMoveListener(new ColorPickerView.OnPinMoveListener() {
              @Override
              public void onPinMove(int color, float x, float y) {
                  tv.setText("R:" + Color.red(color) + " G:" + Color.green(color) + " B:" + Color.blue(color));
              }
          });
  
      }
      ...
  </p>
