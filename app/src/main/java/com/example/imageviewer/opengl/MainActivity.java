package com.example.imageviewer.opengl;

import android.app.Activity;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Window;

import androidx.core.view.WindowCompat;

public final class MainActivity extends Activity {

	private GLSurfaceView mGLSurfaceView;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTransparentStatusBar(getWindow());
		mGLSurfaceView = new GLSurfaceView(this);
		mGLSurfaceView.setEGLContextClientVersion(2);
		mGLSurfaceView.setPreserveEGLContextOnPause(true);
		mGLSurfaceView.setRenderer(new SimpleRenderer(getApplicationContext()));
		setContentView(mGLSurfaceView);
	}

	private void setTransparentStatusBar(Window window) {
		WindowCompat.setDecorFitsSystemWindows(window, false);
		window.setStatusBarColor(Color.TRANSPARENT);
		window.setNavigationBarColor(Color.TRANSPARENT);
	}

	@Override
	public void onResume() {
		super.onResume();
		mGLSurfaceView.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		mGLSurfaceView.onPause();
	}
}