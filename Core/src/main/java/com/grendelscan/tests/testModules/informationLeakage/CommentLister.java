package com.grendelscan.tests.testModules.informationLeakage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.w3c.dom.html2.HTMLScriptElement;

import com.grendelscan.data.database.DataNotFoundException;
import com.grendelscan.data.database.collections.DatabaseBackedList;
import com.grendelscan.data.findings.Finding;
import com.grendelscan.data.findings.FindingSeverity;
import com.grendelscan.html.HtmlNodeUtilities;
import com.grendelscan.requester.http.transactions.StandardHttpTransaction;
import com.grendelscan.scan.Scan;
import com.grendelscan.tests.testModuleUtils.TestModuleGUIPath;
import com.grendelscan.tests.testModuleUtils.settings.MultiSelectOptionGroup;
import com.grendelscan.tests.testModuleUtils.settings.SelectableOption;
import com.grendelscan.tests.testModules.TestModule;
import com.grendelscan.tests.testTypes.ByMimeTypeTest;
import com.grendelscan.utils.MimeUtils;
import com.grendelscan.utils.StringUtils;

public class CommentLister extends TestModule implements ByMimeTypeTest
{

	private MultiSelectOptionGroup		contentTypeGroup;
	private SelectableOption			htmlTypeOption;
	private DatabaseBackedList<String> commentHashes;
	private Pattern						javascriptBlockCommentPattern	= Pattern.compile("/\\*(.+?)\\*/",
																				Pattern.MULTILINE);
	private Pattern						javascriptLineCommentPattern	= Pattern.compile("//(.+)");
	private SelectableOption			javascriptTypeOption;
	private Finding javaScriptFinding;
	private Finding htmlFinding;
	private final static String JAVASCRIPT_FINDING_NAME = "javascript_comments_finding_number";
	private final static String HTML_FINDING_NAME = "html_comments_finding_number";

	public CommentLister()
	{
		commentHashes = new DatabaseBackedList<String>("comment_hashes_list");

		contentTypeGroup = new MultiSelectOptionGroup("Content types", "Content types to search for comments.", null);
		addConfigurationOption(contentTypeGroup);

		htmlTypeOption = new SelectableOption("HTML", true, "Search HTML content for comments.", null);
		contentTypeGroup.addOption(htmlTypeOption);

		javascriptTypeOption = new SelectableOption("JavaScript", true, "Search JavaScript content for comments.", null);
		contentTypeGroup.addOption(javascriptTypeOption);

//		outputFileOption =
//				new FileNameOption("Output file name", "comments.txt", "The file to save the comments in.", false);
//		configOptions.add(outputFileOption);
	}


	@Override
	public String getDescription()
	{
		return "Saves all observed comments into the specified file. Only unique comments are saved; if a comment "
				+ "is seen more than once, only the first instance will be reported. For the sake of performance, JavaScript "
				+ "comment searches aren't very intelligent. For example, something like this will match as a "
				+ "comment:\n" + "    a = \"123/*fake comment*/321\";";
	}

	@Override
	public TestModuleGUIPath getGUIDisplayPath()
	{
		return TestModuleGUIPath.INFORMATION_LEAKAGE;
	}

	@Override
	public String[] getMimeTypes()
	{
		Set<String> types = new HashSet<String>();
		if (htmlTypeOption.isSelected())
		{
			for (String type : MimeUtils.getHtmlMimeTypes())
			{
				types.add(type);
			}
		}

		if (javascriptTypeOption.isSelected())
		{
			for (String type : MimeUtils.getJavaScriptMimeTypes())
			{
				types.add(type);
			}
		}

		return types.toArray(new String[0]);
	}


	@Override
	public String getName()
	{
		return "Comment lister";
	}


	@Override
	public boolean isExperimental()
	{
		return false;
	}

	@Override
	public void testByMimeType(int transactionID, String mimeType, int testJobId)
	{
		StandardHttpTransaction transaction = Scan.getInstance().getTransactionRecord().getTransaction(transactionID);
		if (MimeUtils.isHtmlMimeType(mimeType))
		{
			logFinding(transaction.getRequestWrapper().getAbsoluteUriString(),
					getHtmlComments(transaction.getResponseWrapper().getResponseDOM()), true);
			logFinding(transaction.getRequestWrapper().getAbsoluteUriString(),
					getHtmlScriptComments(transaction.getResponseWrapper().getResponseDOM()), false);
		}

		if (MimeUtils.isJavaScriptMimeType(mimeType))
		{
			logFinding(transaction.getRequestWrapper().getAbsoluteUriString(),
					getJavaScriptComments(new String(transaction.getResponseWrapper().getBody())), false);
		}
	}

//	private String compileComments(Map<String, List<String>> map)
//	{
//		String reportString = "";
//		String urlsArray[] = htmlComments.keySet().toArray(new String[0]);
//		Arrays.sort(urlsArray);
//		for (String url : urlsArray)
//		{
//			if (map.get(url).size() > 0)
//			{
//				reportString += url + "\n";
//				for (String comment : map.get(url))
//				{
//					reportString += "\t" + comment + "\n";
//				}
//				reportString += "\n";
//			}
//		}
//		return reportString;
//	}

	private synchronized void logFinding(String uri, List<String> comments, boolean html)
	{
		Finding finding;
		String name;
		if (html)
		{
			finding = htmlFinding;
			name = HTML_FINDING_NAME;
		}
		else
		{
			finding = javaScriptFinding;
			name = JAVASCRIPT_FINDING_NAME;
		}
		
		try
		{
			if (finding == null)
			{
				finding = Scan.getInstance().getFindings().get(Scan.getInstance().getTestData().getInt(name));
			}
		}
		catch (DataNotFoundException e)
		{
			String type = (html ? "HTML" : "JavaScript");
			String title = type + " comment list";
			String shortDesc = "A list of " + type + " comments was generated";
			String longDesc = "A list of unique " + type + 
				" comments is below. Only the first instance of a comment is listed:<br><br>\n";
			finding = new Finding(null, getName(), FindingSeverity.INFO, "", title, shortDesc,
							longDesc, "", "", "");
			
			Scan.getInstance().getTestData().setInt(name, finding.getId());
			Scan.getInstance().getFindings().addFinding(finding);
		}
		
		String text = finding.getLongDescription();
		
		text += uri + "<br/>\n";
		for(String comment: comments)
		{
			text += "&nbsp;&nbsp;&nbsp;&nbsp;" + comment + "<br/>\n";
		}
		finding.setLongDescription(text);
	}

	private List<String> getHtmlComments(Node node)
	{
		List<String> comments = new ArrayList<String>();
		for (String comment : HtmlNodeUtilities.getAllComments(node))
		{
			recordComment(comment, comments);
		}
		return comments;
	}

	private List<String> getHtmlScriptComments(Node node)
	{
		List<String> comments = new ArrayList<String>();
		for (Node child : HtmlNodeUtilities.getChildElements(node, "SCRIPT"))
		{
			HTMLScriptElement script = (HTMLScriptElement) child;
			for (String comment : getJavaScriptComments(script.getText()))
			{
				recordComment(comment, comments);
			}
		}

		return comments;
	}

	private List<String> getJavaScriptComments(String javaScript)
	{
		List<String> comments = new ArrayList<String>();

		{
			Matcher blockMatcher = javascriptBlockCommentPattern.matcher(javaScript);
			while (blockMatcher.find())
			{
				recordComment(blockMatcher.group(1), comments);
			}
		}

		{
			Matcher lineMatcher = javascriptLineCommentPattern.matcher(javaScript);
			while (lineMatcher.find())
			{
				recordComment(lineMatcher.group(1), comments);
			}
		}

		return comments;
	}

	private synchronized void recordComment(String comment, List<String> comments)
	{
		String hash = StringUtils.md5Hash(comment);
		if (!commentHashes.contains(hash))
		{
			commentHashes.add(hash);
			comments.add(comment);
		}
	}

}
