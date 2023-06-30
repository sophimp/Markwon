package io.noties.markwon.app.samples.basics

import android.content.res.AssetManager
import android.graphics.Color
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import io.noties.debug.Debug
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.RenderProps
import io.noties.markwon.app.readme.GrammarLocatorDef
import io.noties.markwon.app.sample.ui.MarkwonTextViewSample
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.CssInlineStyleParser
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.html.HtmlTag
import io.noties.markwon.html.tag.SimpleTagHandler
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.sample.annotations.MarkwonArtifact
import io.noties.markwon.sample.annotations.MarkwonSampleInfo
import io.noties.markwon.sample.annotations.Tag
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

@MarkwonSampleInfo(
  id = "20200626152255",
  title = "Simple",
  description = "The most primitive and simple way to apply markdown to a `TextView`",
  artifacts = [MarkwonArtifact.CORE],
  tags = [Tag.basics]
)
class Simple : MarkwonTextViewSample() {
  override fun render() {
    // markdown input
//    val md = """
//      # Heading
//
//      > A quote
//
//      **bold _italic_ bold**
//    """.trimIndent()
    val md = getStringFromAssetFile(context.assets, "markdown1.md")

    // markwon instance
    val markwon = Markwon.builder(context)
      .usePlugin(MarkwonInlineParserPlugin.create())
      .usePlugin(ImagesPlugin.create())
      .usePlugin(SyntaxHighlightPlugin.create(Prism4j(GrammarLocatorDef()), Prism4jThemeDefault.create(0)))
      .usePlugin(HtmlPlugin.create { plugin -> plugin.addHandler(SpanTagHandler()) })
      .usePlugin(
        JLatexMathPlugin.create(textView.textSize,
          JLatexMathPlugin.BuilderConfigure { builder: JLatexMathPlugin.Builder ->
            builder.inlinesEnabled(
              true
            )
          })
      )
      .usePlugin(TablePlugin.create(context))
      .build()

    // apply raw markdown (internally parsed and rendered)
    markwon.setMarkdown(textView, md)
  }

  fun getStringFromAssetFile(asset: AssetManager, filename: String?): String {
    var istream: InputStream? = null
    return try {
      istream = asset.open(filename!!)
      getStringFromInputStream(istream)
    } catch (e: Exception) {
      e.printStackTrace()
      ""
    } finally {
      if (istream != null) {
        try {
          istream.close()
        } catch (e: IOException) {
          e.printStackTrace()
        }
      }
    }
  }
  private class SpanTagHandler : SimpleTagHandler() {
    override fun getSpans(
      configuration: MarkwonConfiguration,
      renderProps: RenderProps,
      tag: HtmlTag
    ): Any? {
      val style = tag.attributes()["style"]
      if (TextUtils.isEmpty(style)) {
        return null
      }
      var color = 0
      var backgroundColor = 0
      for (property in CssInlineStyleParser.create().parse(style!!)) {
        when (property.key()) {
          "color" -> color = Color.parseColor(property.value())
          "background-color" -> backgroundColor = Color.parseColor(property.value())
          else -> Debug.i("unexpected CSS property: %s", property)
        }
      }
      val spans: MutableList<Any> = ArrayList(3)
      if (color != 0) {
        spans.add(ForegroundColorSpan(color))
      }
      if (backgroundColor != 0) {
        spans.add(BackgroundColorSpan(backgroundColor))
      }
      return spans.toTypedArray()
    }

    override fun supportedTags(): Collection<String> {
      return setOf("span")
    }
  }
  @Throws(IOException::class)
  fun getStringFromInputStream(istream: InputStream): String {
    val baos = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var length: Int
    while (istream.read(buffer).also { length = it } != -1) {
      baos.write(buffer, 0, length)
    }
    return baos.toString("UTF-8")
  }
}