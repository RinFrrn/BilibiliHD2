# Proguard rules specific to the UI module.

# Constructor method accessed via reflection in TrackSelectionDialogBuilder
-dontnote androidx.appcompat.app.AlertDialog.Builder
-keepclassmembers class androidx.appcompat.app.AlertDialog$Builder {
  <init>(android.content.Context, int);
  public android.content.Context getContext();
  public androidx.appcompat.app.AlertDialog$Builder setTitle(java.lang.CharSequence);
  public androidx.appcompat.app.AlertDialog$Builder setView(android.view.View);
  public androidx.appcompat.app.AlertDialog$Builder setPositiveButton(int, android.content.DialogInterface$OnClickListener);
  public androidx.appcompat.app.AlertDialog$Builder setNegativeButton(int, android.content.DialogInterface$OnClickListener);
  public androidx.appcompat.app.AlertDialog create();
}

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn kotlin.annotations.jvm.**
-dontwarn javax.annotation.**