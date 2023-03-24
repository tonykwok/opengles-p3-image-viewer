package com.example.imageviewer.opengl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

/**
 * ビットマップをテクスチャとして読み込んで {@link GLSurfaceView} の描画領域いっぱいに描画するだけのシンプルな {@link GLSurfaceView.Renderer} を提供します。
 * 
 * @author 杉澤 浩二
 */
public final class SimpleRenderer implements GLSurfaceView.Renderer {

	/**
	 * コンテキストを保持します。
	 */
	private final Context mContext;

	/**
	 * 頂点データです。
	 */
	private static final float VERTEXS[] = {
		-1.0f,  1.0f, 0.0f,	// 左上
		-1.0f, -1.0f, 0.0f,	// 左下
		 1.0f,  1.0f, 0.0f,	// 右上
		 1.0f, -1.0f, 0.0f	// 右下
	};

	/**
	 * テクスチャ (UV マッピング) データです。
	 */
	private static final float TEXCOORDS[] = {
		0.0f, 0.0f,	// 左上
		0.0f, 1.0f,	// 左下
		1.0f, 0.0f,	// 右上
		1.0f, 1.0f	// 右下
	};

	/**
	 * 頂点バッファを保持します。
	 */
	private final FloatBuffer mVertexBuffer   = GLES20Utils.createBuffer(VERTEXS);

	/**
	 * テクスチャ (UV マッピング) バッファを保持します。
	 */
	private final FloatBuffer mTexcoordBuffer = GLES20Utils.createBuffer(TEXCOORDS);

	private int mProgram;
	private int mPosition;
	private int mTexcoord;
	private int mTexture;

	private int mTextureId;

	//////////////////////////////////////////////////////////////////////////
	// コンストラクタ

	/**
	 * コンストラクタです。
	 * 
	 * @param context コンテキスト
	 */
	public SimpleRenderer(final Context context) {
		mContext = context;
	}

	//////////////////////////////////////////////////////////////////////////
	// オーバーライド メソッド

	/**
	 * ポリゴン描画用のバーテックスシェーダ (頂点シェーダ) のソースコード
	 */
	private static final String VERTEX_SHADER =
		"attribute vec4 position;" +
		"attribute vec2 texcoord;" +
		"varying vec2 texcoordVarying;" +
		"void main() {" +
			"gl_Position = position;" +
			"texcoordVarying = texcoord;" +
		"}";

	/**
	 * 色描画用のピクセル/フラグメントシェーダのソースコード
	 */
	private static final String FRAGMENT_SHADER =
		"precision mediump float;" +
		"varying vec2 texcoordVarying;" +
		"uniform sampler2D texture;" +

		"const mat3 CSC_XYZ_TO_SRGB = mat3(\n" +
		"     3.240969941904521, -0.9692436362808798,  0.05563007969699361,\n" +
		"    -1.537383177570093,  1.87596750150772,   -0.2039769588889765,\n" +
		"    -0.4986107602930033, 0.04155505740717561, 1.056971514242878\n" +
		");\n" +

		"const mat3 CSC_DISPLAY_P3_TO_XYZ = mat3(\n" +
		"    0.4865709486482162, 0.2289745640697488, 0.0,\n" +
		"    0.2656676931690929, 0.6917385218365062, 0.04511338185890257,\n" +
		"    0.1982172852343625, 0.079286914093745,  1.04394436890097500\n" +
		");\n" +

		"float oetf_inverse_sRGB(float rgb) {\n" +
		"    return rgb <= 0.04045 ? rgb / 12.92 : pow((rgb + 0.055) / 1.055, 2.4);\n" +
		"}\n" +

		"float oetf_sRGB(float linear) {\n" +
		"    return linear <= 0.0031308 ? 12.92 * linear : 1.055 * pow(linear, 1.0 / 2.4) - 0.055;\n" +
		"}\n" +

		"void main() {" +
		"    vec4 color = texture2D(texture, texcoordVarying);\n" +
		"    color.r = oetf_inverse_sRGB(color.r);\n" +
		"    color.g = oetf_inverse_sRGB(color.g);\n" +
		"    color.b = oetf_inverse_sRGB(color.b);\n" +
		"    vec3 xyz = CSC_DISPLAY_P3_TO_XYZ * color.rgb;\n" +
		"    vec3 rgb = CSC_XYZ_TO_SRGB * xyz;\n" +
		"    color.r = oetf_sRGB(rgb.r);\n" +
		"    color.g = oetf_sRGB(rgb.g);\n" +
		"    color.b = oetf_sRGB(rgb.b);\n" +
		"    gl_FragColor = color; \n" +
//		"    gl_FragColor = texture2D(texture, texcoordVarying);" +
		"}";

	@Override
	public void onSurfaceCreated(final GL10 gl, final EGLConfig config) {
		// OpenGL ES 2.0 を使用するので、パラメータで渡された GL10 インターフェースを無視して、代わりに GLES20 クラスの静的メソッドを使用します。

		// プログラムを生成して使用可能にします。
		mProgram = GLES20Utils.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
		if (mProgram == 0) {
			throw new IllegalStateException();
		}
		GLES20.glUseProgram(mProgram);
		GLES20Utils.checkGlError("glUseProgram");

		// シェーダで使用する変数のハンドルを取得し使用可能にします。

		mPosition = GLES20.glGetAttribLocation(mProgram, "position");
		GLES20Utils.checkGlError("glGetAttribLocation position");
		if (mPosition == -1) {
			throw new IllegalStateException("Could not get attrib location for position");
		}
		GLES20.glEnableVertexAttribArray(mPosition);

		mTexcoord = GLES20.glGetAttribLocation(mProgram, "texcoord");
		GLES20Utils.checkGlError("glGetAttribLocation texcoord");
		if (mPosition == -1) {
			throw new IllegalStateException("Could not get attrib location for texcoord");
		}
		GLES20.glEnableVertexAttribArray(mTexcoord);

		mTexture = GLES20.glGetUniformLocation(mProgram, "texture");
		GLES20Utils.checkGlError("glGetUniformLocation texture");
		if (mTexture == -1) {
			throw new IllegalStateException("Could not get uniform location for texture");
		}

		// テクスチャを作成します。(サーフェスが作成される度にこれを行う必要があります)
		try (InputStream is = mContext.getAssets().open("P3XM.png")) {
			final Bitmap bitmap = BitmapFactory.decodeStream(is);
			mTextureId = GLES20Utils.loadTexture(bitmap);
			bitmap.recycle();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
//		final Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sample);
//		mTextureId = GLES20Utils.loadTexture(bitmap);
//		bitmap.recycle();
	}

	@Override
	public void onSurfaceChanged(final GL10 gl, final int width, final int height) {
		// OpenGL ES 2.0 を使用するので、パラメータで渡された GL10 インターフェースを無視して、代わりに GLES20 クラスの静的メソッドを使用します。

		// ビューポートを設定します。
		GLES20.glViewport(0, 0, width, height);
		// TODO(tony): crash when activity pause/resume
		// GLES20Utils.checkGlError("glViewport");
	}

	@Override
	public void onDrawFrame(final GL10 gl) {
		// OpenGL ES 2.0 を使用するので、パラメータで渡された GL10 インターフェースを無視して、代わりに GLES20 クラスの静的メソッドを使用します。

		// XXX - このサンプルではテクスチャの簡単な描画だけなので深さ関連の有効/無効や指定は一切していません。

		// 背景色を指定して背景を描画します。
		GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		// 背景とのブレンド方法を設定します。
		GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);	// 単純なアルファブレンド

		// テクスチャの指定
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
		GLES20.glUniform1i(mTexture, 0);
		GLES20.glVertexAttribPointer(mTexcoord, 2, GLES20.GL_FLOAT, false, 0, mTexcoordBuffer);
		GLES20.glVertexAttribPointer(mPosition, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_TEXTURE_2D);
	}

}