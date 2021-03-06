package com.grendelscan.commons.http.responseCompare;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.grendelscan.commons.MimeUtils;
import com.grendelscan.commons.StringUtils;
import com.grendelscan.commons.http.HttpUtils;
import com.grendelscan.commons.http.URIStringUtils;
import com.grendelscan.commons.http.apache_overrides.serializable.SerializableHttpHeader;
import com.grendelscan.commons.http.transactions.StandardHttpTransaction;
import com.grendelscan.commons.http.wrappers.HttpResponseWrapper;

public class HttpResponseScoreUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpResponseScoreUtils.class);

    private static HttpResponseWrapper getCorrectResponse(final StandardHttpTransaction transactionA, final boolean followRedirects)
    {
        if (followRedirects)
        {
            return transactionA.getUltimateResponseWrapper();
        }
        return transactionA.getResponseWrapper();
    }

    private static void getLocationHeaderScore(final HttpResponseWrapper responseA, final HttpResponseWrapper responseB, final ResponseCompareScore score, final HttpTransactionMatchCriteriaWeights weights) throws DoneComparingException
    {
        List<SerializableHttpHeader> locationsA = responseA.getHeaders().getHeaders(HttpHeaders.LOCATION);
        List<SerializableHttpHeader> locationsB = responseB.getHeaders().getHeaders(HttpHeaders.LOCATION);

        // If there is at least one location header, increase the max score
        if (locationsA.size() > 0 || locationsB.size() > 0)
        {
            score.incMaxScore(weights.getLocationHeader());
        }
        else
        {
            return;
        }

        /*
         * If only one transaction has a location header, or if they don't have the same number of location headers (real weirdness), give it a zero
         */
        if (locationsA.size() == 0 || locationsA.size() != locationsB.size())
        {
            // no point in looking at the bodies
            throw new DoneComparingException("Mismatched location header count.");
        }

        int perfectScore = 10000;
        int headerScore = StringUtils.scoreStringDifference(locationsA.get(0).getValue(), locationsB.get(0).getValue(), perfectScore);

        /*
         * perfectScore*2 because a complete mismatch (i.e. header only present in one response) should be much more damaging than a slight mismatch
         */
        double diff = 1 - (double) (perfectScore - headerScore) / (double) (perfectScore * 2);
        score.incScore((int) (diff * weights.getLocationHeader()));

        // There's no point in looking at the bodies
        throw new DoneComparingException("Both responses have location headers.");
    }

    private static void getMimeTypeScore(final HttpResponseWrapper responseA, final HttpResponseWrapper responseB, final ResponseCompareScore score, final HttpTransactionMatchCriteriaWeights weights) throws DoneComparingException
    {
        score.incMaxScore(weights.getMimeType());
        if (responseA.getHeaders().getMimeType().equalsIgnoreCase(responseB.getHeaders().getMimeType()))
        {
            score.incScore(weights.getMimeType());
        }
        else if (!responseA.getHeaders().getMimeType().equals("") && !responseB.getHeaders().getMimeType().equals(""))
        {
            score.incScore(0 - weights.getMimeType());
            throw new DoneComparingException("MIME types don't match.");
        }
    }

    private static void getResponseCodeScore(final HttpResponseWrapper responseA, final HttpResponseWrapper responseB, final ResponseCompareScore score, final HttpTransactionMatchCriteriaWeights weights) throws DoneComparingException
    {
        score.incMaxScore(weights.getResponseCode());
        if (responseA.getStatusLine().getStatusCode() == responseB.getStatusLine().getStatusCode())
        {
            score.incScore(weights.getResponseCode());
        }
        else
        {
            score.incScore(0 - weights.getResponseCode());
            throw new DoneComparingException("Response codes don't match.");
        }
    }

    private static void getSetCookieScore(final StandardHttpTransaction transactionA, final StandardHttpTransaction transactionB, final ResponseCompareScore score, final HttpTransactionMatchCriteriaWeights weights)
    {
        Set<String> setCookieNamesA = HttpUtils.getSetCookieNames(transactionA);
        Set<String> setCookieNamesB = HttpUtils.getSetCookieNames(transactionB);
        Set<String> usedCookieNamesA = transactionA.getUsedCookieNames();
        Set<String> usedCookieNamesB = transactionB.getUsedCookieNames();
        Set<String> allCookieNames = new HashSet<String>();
        allCookieNames.addAll(setCookieNamesA);
        allCookieNames.addAll(setCookieNamesB);

        // If no cookies were set in either transaction, skip this test
        if (allCookieNames.size() == 0)
        {
            return;
        }

        score.incMaxScore(weights.getSetCookieName());

        double tmpScore = 1;

        double increment = (double) 1 / (double) allCookieNames.size();

        for (String name : setCookieNamesA)
        {
            if (!setCookieNamesB.contains(name) & !usedCookieNamesB.contains(name))
            {
                tmpScore -= increment;
            }
        }
        for (String name : setCookieNamesB)
        {
            if (!setCookieNamesA.contains(name) & !usedCookieNamesA.contains(name))
            {
                tmpScore -= increment;
            }
        }

        score.incScore((int) (weights.getSetCookieName() * tmpScore));
    }

    private static void getTagCountScore(final String tagName, final StandardHttpTransaction transactionA, final StandardHttpTransaction transactionB, final ResponseCompareScore score, final HttpTransactionMatchCriteriaWeights weights,
                    final boolean followRedirects)
    {
        Document domA = getCorrectResponse(transactionA, followRedirects).getResponseDOM();
        Document domB = getCorrectResponse(transactionB, followRedirects && transactionA.getRedirectChildId() > 0).getResponseDOM();

        if (domA == null || domB == null)
        {
            // No point in doing this test if one of them isn't HTML
            return;
        }
        int countA = domA.getElementsByTagName(tagName).getLength();
        int countB = domB.getElementsByTagName(tagName).getLength();

        int max = Math.max(countA, countB);
        int min = Math.min(countA, countB);

        // No point in doing the test if neither have this tag
        if (max == 0)
        {
            return;
        }

        score.incMaxScore(weights.getTagCounts().get(tagName));
        double ratio = (double) min / (double) max;
        score.incScore((int) (weights.getTagCounts().get(tagName) * ratio));
    }

    private static void getTagCountsScore(final StandardHttpTransaction transactionA, final StandardHttpTransaction transactionB, final ResponseCompareScore score, final HttpTransactionMatchCriteriaWeights weights, final boolean followRedirects)
    {
        for (String tagName : weights.getTagCounts().keySet())
        {
            getTagCountScore(tagName, transactionA, transactionB, score, weights, followRedirects);
        }
    }

    private static void getTextNodesScore(final StandardHttpTransaction transactionA, final StandardHttpTransaction transactionB, final ResponseCompareScore score, final HttpTransactionMatchCriteriaWeights weights, final boolean followRedirects)
    {
        String textA = normalizeResponseTextContent(transactionA, followRedirects);
        String textB = normalizeResponseTextContent(transactionB, followRedirects && transactionA.getRedirectChildId() > 0);

        /*
         * If there are no response body text nodes, don't do the test. This is usually because there is no body, or because it isn't HTML.
         */
        if (textA.length() == 0 && textB.length() == 0)
        {
            return;
        }

        score.incMaxScore(weights.getTextNodes());

        // Total fail if only one of the bodies is blank
        if (textA.length() == 0 || textB.length() == 0)
        {
            return;
        }

        int perfectScore = 10000;
        double diff = (double) (perfectScore - StringUtils.scoreStringDifference(textA, textB, perfectScore)) / (double) perfectScore;
        score.incScore((int) (weights.getTextNodes() * (1 - diff)));
    }

    private static String normalizeResponseTextContent(final StandardHttpTransaction transaction, final boolean followRedirects)
    {
        String newResponse;
        HttpResponseWrapper responseWrapper = getCorrectResponse(transaction, followRedirects);
        if (MimeUtils.isHtmlMimeType(responseWrapper.getHeaders().getMimeType()))
        {
            String uri = transaction.getRequestWrapper().getAbsoluteUriString();
            newResponse = responseWrapper.getStrippedResponseText();

            newResponse = newResponse.toUpperCase();
            try
            {
                newResponse = newResponse.replace(URIStringUtils.getDirectory(uri).toUpperCase(), "");
                newResponse = newResponse.replace(URIStringUtils.getFilename(uri).toUpperCase(), "");
                newResponse = newResponse.replace("?" + URIStringUtils.getQuery(uri).toUpperCase(), "");
            }
            catch (URISyntaxException e)
            {
                IllegalStateException ise = new IllegalStateException("Really, really weird problem with uri parsing", e);
                LOGGER.error(e.toString(), e);
                throw ise;
            }

            // Get rid of time stamps
            newResponse = newResponse.replaceAll("[0-2]\\d:[0-5]\\d(?::[0-5]\\d)?(?:\\s*[aApP]\\.?[mM]\\.?)", "");

            // Get rid of extra whitespace
            newResponse = newResponse.replaceAll("[\r\n\\s]++", " ");

        }
        else
        {
            newResponse = "";
        }

        return newResponse;
    }

    private static ResponseCompareScore scoreResponseMatch(final StandardHttpTransaction transactionA, final StandardHttpTransaction transactionB, final HttpTransactionMatchCriteriaWeights weights, final boolean useDOM, final boolean followRedirects)
    {
        ResponseCompareScore score = new ResponseCompareScore();
        if (!transactionA.isSuccessfullExecution() || !transactionB.isSuccessfullExecution())
        {
            score.incMaxScore(1);
            return score;
        }
        HttpResponseWrapper responseA = getCorrectResponse(transactionA, followRedirects);
        HttpResponseWrapper responseB = getCorrectResponse(transactionB, followRedirects && transactionA.getRedirectChildId() > 0);

        try
        {
            getResponseCodeScore(responseA, responseB, score, weights);
            getMimeTypeScore(responseA, responseB, score, weights);
            getLocationHeaderScore(responseA, responseB, score, weights);
            getSetCookieScore(transactionA, transactionB, score, weights);
            if (useDOM)
            {
                getTextNodesScore(transactionA, transactionB, score, weights, followRedirects);
                getTagCountsScore(transactionA, transactionB, score, weights, followRedirects);
            }
        }
        catch (DoneComparingException e)
        {
            // Don't care. The exception is just a signal to stop
        }
        return score;
    }

    public static int scoreResponseMatch(final StandardHttpTransaction transactionA, final StandardHttpTransaction transactionB, final HttpTransactionMatchCriteriaWeights weights, final int maxScore, final boolean useDOM, final boolean followRedirects)
    {
        ResponseCompareScore score = scoreResponseMatch(transactionA, transactionB, weights, useDOM, followRedirects);
        return (int) Math.round(score.getPercentage() * maxScore);
    }

    public static int scoreResponseMatch(final StandardHttpTransaction transactionA, final StandardHttpTransaction transactionB, final int maxScore, final boolean useDOM, final boolean followRedirects)
    {
        HttpTransactionMatchCriteriaWeights weights = new HttpTransactionMatchCriteriaWeights();
        return scoreResponseMatch(transactionA, transactionB, weights, maxScore, useDOM, followRedirects);
    }
}
