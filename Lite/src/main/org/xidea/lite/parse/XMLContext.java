package org.xidea.lite.parse;

import java.io.IOException;
import java.net.URI;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xidea.lite.impl.ParseContextImpl;
import org.xidea.lite.impl.XMLContextImpl;
import org.xml.sax.SAXException;
/**
 * @author jindw
 * 要设计为继承安全，这个接口不能滥用
 * 而且，实现不能直接调用this。否则容易形成孤岛
 */
public interface XMLContext {

	/**
	 * 如果compress为真，或者 reserveSpace为真 则该属性失效
	 * 
	 * @param format
	 */
	public boolean isFormat();

	public void setFormat(boolean format);


	/**
	 * 如果 reserveSpace为真 则该属性失效
	 * 
	 * @return
	 */
	public boolean isCompress();

	public void setCompress(boolean compress);

	/**
	 * 该属性为真时，compress 和 format都将失效
	 * 
	 * @return
	 */
	public boolean isReserveSpace();

	public void setReserveSpace(boolean keepSpace);

	/**
	 * 开始缩进(当压缩属性和reserveSpace都不为真的时候有效)
	 * 
	 * @see ParseContextImpl#beginIndent()
	 */
	public void beginIndent();

	/**
	 * 开始缩进(当压缩属性和reserveSpace都不为真的时候有效)
	 * 
	 * @see ParseContextImpl#endIndent()
	 */
	public void endIndent();

	/**
	 * 装载指定文档。数据源需要从ResourceContext中获取资源数据
	 * @see ResourceContext#openStream(parentURI)
	 * @param createURI
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document loadXML(URI parentURI) throws SAXException, IOException;

	/**
	 * @param doc
	 * @param xpath
	 * @return
	 * @throws XPathExpressionException
	 */
	public DocumentFragment selectNodes(Node doc,String xpath)
			throws XPathExpressionException;

	/**
	 * @param parentURI
	 * @param doc
	 * @param xslt
	 * @see XMLContextImpl#transform(URI, Node, Node)
	 * @return
	 * @throws TransformerConfigurationException
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 * @throws IOException
	 */
	public Node transform(Node doc, Node xslt)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException,
			IOException;

}