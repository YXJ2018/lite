package org.xidea.lite.parser.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.Template;
import org.xidea.lite.parser.Parser;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;

public class CoreXMLParser implements Parser<Element> {
	private static Log log = LogFactory.getLog(CoreXMLParser.class);
	private static final Pattern TEMPLATE_NAMESPACE_CORE = Pattern
			.compile("^http:\\/\\/www.xidea.org\\/ns\\/(?:template|lite)(?:\\/core)?\\/?$");
	private JSBuilder jsBuilder;
	private ExpressionFactory jselFactory;

	public static boolean isCoreNS(String prefix, String url) {
		return ("c".equals(prefix) && ("#".equals(url) || "#core".equals(url)))
				|| TEMPLATE_NAMESPACE_CORE.matcher(url).find();
	}

	public CoreXMLParser() {
		this.jselFactory = new ExpressionFactory() {
				public Expression create(Object el) {
					throw new UnsupportedOperationException();
				}

				public Object parse(String expression) {
					return expression;
				}

			};
		try {
			jsBuilder = new RhinoJSBuilder();
		} catch (NoClassDefFoundError e) {
			try {
				jsBuilder = new Java6JSBuilder();
			} catch (NoClassDefFoundError e2) {
				log.error("找不到您的JS运行环境，不能为您编译前端js", e);

			}
		}
	}
	public CoreXMLParser(ExpressionFactory jselFactory,JSBuilder jsbuilder) {
		this.jselFactory = jselFactory;
		this.jsBuilder = jsbuilder;
	}

	public void parse( ParseContext context,ParseChain chain,final Element el) {
			String prefix = el.getPrefix();
			String namespaceURI = el.getNamespaceURI();
			if (namespaceURI != null && isCoreNS(prefix, namespaceURI)) {
				String name = el.getLocalName();
				if ("include".equals(name)) {
					 parseIncludeTag(el, context);
				} else if ("client".equals(name)) {
					parseClientTag(el, context);
				} else if ("group".equals(name) || "context".equals(name)) {
					parseContextTag(el, context);
				} else if ("json".equals(name)) {
					parseJSONTag(el, context);
				} else if ("choose".equals(name)) {
					parseChooseTag(el, context);
				} else if ("elseif".equals(name) || "else-if".equals(name)
						|| "elif".equals(name)) {
					parseElseIfTag(el, context, true);
				} else if ("else".equals(name)) {
					parseElseIfTag(el, context, false);
				} else if ("if".equals(name)) {
					parseIfTag(el, context);
				} else if ("out".equals(name)) {
					parseOutTag(el, context);
				} else if ("for".equals(name) || "forEach".equals(name)
						|| "for-each".equals(name)) {
					parseForTag(el, context);
				} else if ("var".equals(name)) {
					parseVarTag(el, context);
				}else{
					log.error("未知核心标记" +name);
					chain.process(el);
				}
			}else{
				chain.process(el);
			}
	}

	public void parseIncludeTag(final Element el, ParseContext context) {
		String var = getAttributeOrNull(el, "var");
		String path = getAttributeOrNull(el, "path");
		String xpath = getAttributeOrNull(el, "xpath");
		String xslt = getAttributeOrNull(el, "xslt");
		String name = getAttributeOrNull(el, "name");
		Node doc = el.getOwnerDocument();
		final URL parentURL = context.getCurrentURL();
		try {
			if (name != null) {
				Node cachedNode = XMLContextImpl.toDocumentFragment(el, el
						.getChildNodes());
				context.setAttribute("#" + name, cachedNode);
			}
			if (var != null) {
				context.appendCaptrue(var);
				context.parse(el.getChildNodes());
				context.appendEnd();
			}
			if (path != null) {
				if (path.startsWith("#")) {
					doc = (Node) context.getAttribute(path);
					String uri;
					if (doc instanceof Document) {
						uri = ((Document) doc).getDocumentURI();
					} else {
						uri = doc.getOwnerDocument().getDocumentURI();
					}
					if (uri != null) {
						context.setCurrentURL(context.createURL(null, uri));
					}
				} else {
					doc = context.loadXML(context
							.createURL(parentURL, path));
				}
			}

			if (xpath != null) {
				doc = context.selectNodes(xpath, doc);
			}
			if (xslt != null) {
				doc = context.transform(parentURL, doc, xslt);
			}
			context.parse(doc);
		} catch (Exception e) {
			log.warn(e);
		} finally {
			context.setCurrentURL(parentURL);
		}
	}


	protected Node parseIfTag(Element el, ParseContext context) {
		Object test = getAttributeEL(context, el, "test");
		context.appendIf(test);
		parseChild(el.getFirstChild(), context);
		context.appendEnd();
		return null;
	}

	protected Node parseElseIfTag(Element el, ParseContext context, boolean reqiiredTest) {
		context.clearPreviousText();
		if (((Element) el).hasAttribute("test")) {
			Object test = getAttributeEL(context, el, "test");
			context.appendElse(test);
		} else if (reqiiredTest) {
			throw new IllegalArgumentException("@test is required");
		} else {
			context.appendElse(null);
		}
		parseChild(el.getFirstChild(), context);
		context.appendEnd();
		return null;
	}

	protected Node parseElseTag(Element el, ParseContext context) {
		context.clearPreviousText();
		context.appendElse(null);
		parseChild(el.getFirstChild(), context);
		context.appendEnd();
		return null;
	}

	protected Node parseChooseTag(final Element el2, ParseContext context) {
		boolean first = true;
		String whenTag = "when";
		String elseTag = "otherwise";
		Node next = el2.getFirstChild();
		while (next != null) {
			if (next instanceof Element) {
				// ignore namespace check
				if (whenTag.equals(next.getLocalName())) {
					if (first) {
						first = false;
						parseIfTag((Element) next, context);
					} else {
						parseElseIfTag((Element) next, context, true);
					}
				} else if (next.getLocalName().equals(elseTag)) {
					parseElseTag((Element) next, context);
				} else {
					throw new RuntimeException("choose 只接受when，otherwise 节点");
				}
			}
			next = next.getNextSibling();
		}
		return null;
	}

	protected Node parseForTag(Element el, ParseContext context) {
		Object items = getAttributeEL(context, el, "items");
		String var = getAttributeOrNull(el, "var");
		String status = getAttributeOrNull(el, "status");
		context.appendFor(var, items, status);
		parseChild(el.getFirstChild(), context);
		context.appendEnd();
		return null;
	}

	protected Node parseVarTag(Element el, ParseContext context) {
		String name = getAttributeOrNull(el, "name");
		String value = getAttributeOrNull(el, "value");
		if (value == null) {
			context.appendCaptrue(name);
			parseChild(el.getFirstChild(), context);
			context.appendEnd();
		} else {
			int mark = context.mark();
			context.parseText(value, Template.EL_TYPE);
			List<Object> temp = context.reset(mark);
			if (temp.size() == 1) {
				Object item = temp.get(0);
				if(item instanceof Object[]){//EL_TYPE
					context.appendVar(name, ((Object[])item)[1]);
				}else{
					context.appendVar(name, context.parseEL(JSONEncoder.encode(item)));
				}
			} else {
				context.appendCaptrue(name);
				context.appendAll(temp);
				context.appendEnd();
			}
		}
		return null;
	}

	protected void parseClientTag(Element el, ParseContext context) {
		Node next = el.getFirstChild();
		if (next != null && jsBuilder != null) {
			// new Java6JSBuilder();
			ParseContext context2 = new ParseContextImpl(context
					.getCurrentURL());
			// 前端直接压缩吧？反正保留那些空白也没有调试价值
			// context2.setCompress(context.isCompress());
			context2.setCompress(true);
			context2.setExpressionFactory(jselFactory);
			do {
				context2.parse(next);
			} while ((next = next.getNextSibling()) != null);
			List<Object> result = context2.toResultTree();
			String js = jsBuilder.buildJS(el.getAttribute("id"), result);
			if (context.isCompress()) {
				js = jsBuilder.compress(js);
			}
			boolean needScript = needScript(el);
			if (needScript) {
				context.append("<script>/*<![CDATA[*/" + js
						+ "/*]]>*/</script>");
			} else {
				context.append("/*<![CDATA[*/" + js + "/*]]>*/");
			}
		}
	}

	private boolean needScript(Element el) {
		return true;
	}

	protected void parseContextTag(Element el, ParseContext context) {
		parseChild(el.getFirstChild(), context);
	}

	protected void parseJSONTag(final Element el, ParseContext context) {
		String var = getAttributeOrNull(el, "var");
		String file = getAttributeOrNull(el, "file");
		String encoding = getAttributeOrNull(el, "encoding", "charset");
		String content = getAttributeOrNull(el, "content");
		if (file != null) {
			try {
				URL url = context.createURL(null, file);
				InputStream in = url.openStream();
				InputStreamReader reader = new InputStreamReader(in,
						encoding == null ? "utf-8" : encoding);
				StringBuilder sbuf = new StringBuilder();
				char[] cbuf = new char[1024];
				int c;
				while ((c = reader.read(cbuf)) >= 0) {
					sbuf.append(cbuf, 0, c);
				}
				content = sbuf.toString();
			} catch (Exception e) {
				if (log.isWarnEnabled()) {
					log.warn("json文件读取失败", e);
				}
			}
		}
		if (content == null) {
			// Node next = node.getFirstChild();
			// context.append(new Object[] { VAR_TYPE, var, null });
			// if (next != null) {
			// do {
			// this.parser.parseNode(next, context);
			// } while ((next = next.getNextSibling()) != null);
			// }
			// context.appendEnd();
			content = el.getTextContent();
		}
		context.appendVar(var, context.parseEL(content));
	}

	protected void parseOutTag(Element el, ParseContext context) {
		String value = getAttributeOrNull(el, "value");
		context.parseText(value,Template.EL_TYPE);
	}

	private void parseChild(Node child, ParseContext context) {
		while (child != null) {
			context.parse(child);
			child = child.getNextSibling();
		}
	}

	private String getAttributeOrNull(Element el, String... keys) {
		for (String key : keys) {
			if (el.hasAttribute(key)) {
				return el.getAttribute(key);
			}
		}
		return null;
	}

	private Object toEL(ParseContext context, String value) {
		value = value.trim();
		if (value.startsWith("${") && value.endsWith("}")) {
			value = value.substring(2, value.length() - 1);
		} else {
			log.warn("输入的不是有效el，系统将字符串转换成el");
			value = JSONEncoder.encode(value);
		}
		return context.parseEL(value);
	}

	private Object getAttributeEL(ParseContext context, Element el, String key) {
		String value = getAttributeOrNull(el, key);
		return toEL(context, value);

	}
}