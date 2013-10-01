package com.grendelscan.tests.libraries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.w3c.dom.Node;

import com.grendelscan.data.database.collections.DatabaseBackedMap;
import com.grendelscan.data.findings.FindingsCollection;
import com.grendelscan.html.HtmlNodeWriter;
import com.grendelscan.logging.Log;
import com.grendelscan.scan.ConfigurationManager;
import com.grendelscan.utils.StringUtils;

public class SQLInjection
{
	public final static String						DB2			= "IBM DB2";
	public final static String						GENERAL		= "unknown";
	public final static String						MSACCESS	= "Microsoft Access";
	public final static String						MSSQL		= "Microsoft SQL Server";
	public final static String						MYSQL		= "MySQL";
	public final static String						ORACLE		= "Oracle";
	public final static String						POSTGRESQL	= "PostgreSQL";
	private static HashMap<String, Set<Pattern>>	errorRegexs;
	private static Set<Pattern>						generalErrorRegexs;
	private static boolean							initialized	= false;
	private static Map<String, String>				numericTautologies;
	private static Map<String, String>				stringTautologies;
	private static float							tautologyThreshold;
	

	public static String findSQLErrorMessages(Node node)
	{
		initializeLibrary();
		return findSQLErrorMessages(HtmlNodeWriter.writeTextOnly(node, true));
	}

	/**
	 * Returns the database platform constant (e.g. SQLInjection.ORACLE), or a
	 * null
	 * if no error was found
	 * 
	 * @param text
	 * @return
	 */
	public static String findSQLErrorMessages(String text)
	{
		initializeLibrary();
		if ((text == null) || text.equals(""))
		{
			return null;
		}

		text = StringUtils.normalizeWhiteSpace(text);

		for (String platform : errorRegexs.keySet())
		{
			for (Pattern pattern : errorRegexs.get(platform))
			{
				Matcher m = pattern.matcher(text);
				if (m.find())
				{
					return platform;
				}
			}
		}

		// general needs to be tested _after_ all of the platforms are tested
		for (Pattern pattern : generalErrorRegexs)
		{
			Matcher m = pattern.matcher(text);
			if (m.find())
			{
				return SQLInjection.GENERAL;
			}
		}

		return null;
	}

	public static Map<String, String> getNumericTautologies()
	{
		initializeLibrary();
		return numericTautologies;
	}

	public static String getSQLInjectionImpact()
	{
		String r =
				"SQL injection can allow an attacker to obtain the information stored in your database. Under some circumstances "
						+
						"it can also allow for arbitrary command execution on the database server.";
		return r;
	}

	public static String getSQLInjectionRecomendations()
	{
		String r =
				"SQL injection is best prevented by using prepared queries supported by your database driver, or using "
						+
						"parameterized stored procedures. Combining both provides the best protection. Using whitelists or "
						+
						"blacklists of characters is problematic with all but the simplest of data types (i.e. all numbers).";
		return r;
	}

	public static String getSQLInjectionReferences()
	{
		String r =
				"http://www.owasp.org/index.php/SQL_Injection<br>http://www.owasp.org/index.php/Testing_for_SQL_Injection";
		return r;
	}

	public static Map<String, String> getStringTautologies()
	{
		initializeLibrary();
		return stringTautologies;
	}

	public static float getTautologyThreshold()
	{
		initializeLibrary();
		return tautologyThreshold;
	}

	private static Set<Pattern> generatePatterns(List<String> rawPatterns)
	{
		Set<Pattern> patterns = new HashSet<Pattern>();
		for (String stringPattern : rawPatterns)
		{
			try
			{
				Pattern pattern = Pattern.compile(stringPattern);
				patterns.add(pattern);
			}
			catch (PatternSyntaxException e)
			{
				Log.error("Problem loading a SQL error pattern: " + e.toString(), e);
			}
		}
		return patterns;
	}

	private synchronized static void initializeLibrary()
	{
		if (!initialized)
		{
			initialized = true;
			loadTautologies();
			errorRegexs = new HashMap<String, Set<Pattern>>(7);
			generalErrorRegexs =
					generatePatterns(ConfigurationManager.getList("sql_injection.error_message_regexs.general"));
			errorRegexs.put(SQLInjection.MSSQL,
					generatePatterns(ConfigurationManager.getList("sql_injection.error_message_regexs.mssql")));
			errorRegexs.put(SQLInjection.MYSQL,
					generatePatterns(ConfigurationManager.getList("sql_injection.error_message_regexs.mysql")));
			errorRegexs.put(SQLInjection.POSTGRESQL,
					generatePatterns(ConfigurationManager.getList("sql_injection.error_message_regexs.postgresql")));
			errorRegexs.put(SQLInjection.ORACLE,
					generatePatterns(ConfigurationManager.getList("sql_injection.error_message_regexs.oracle")));
			errorRegexs.put(SQLInjection.MSACCESS,
					generatePatterns(ConfigurationManager.getList("sql_injection.error_message_regexs.access")));
			errorRegexs.put(SQLInjection.DB2,
					generatePatterns(ConfigurationManager.getList("sql_injection.error_message_regexs.db2")));
			tautologyThreshold = ConfigurationManager.getFloat("sql_injection.tautology_threshold");
		}
	}

	private static void loadTautologies()
	{
		numericTautologies = loadTautologyType("sql_injection.numeric_tautologies");
		stringTautologies = loadTautologyType("sql_injection.string_tautologies");
	}

	private static Map<String, String> loadTautologyType(String configPath)
	{
		Map<String, String> tautologies = new HashMap<String, String>();
		String tauts[] = ConfigurationManager.getStringArray(configPath);
		for (int index = 0; index < tauts.length - 1; index += 2)
		{
			tautologies.put(tauts[index], tauts[index + 1]);
		}

		return tautologies;
	}


}
