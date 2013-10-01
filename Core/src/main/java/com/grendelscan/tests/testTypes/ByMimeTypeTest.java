package com.grendelscan.tests.testTypes;

import com.grendelscan.scan.InterruptedScanException;

public interface ByMimeTypeTest extends TestType
{
	public String[] getMimeTypes();

	public void testByMimeType(int transactionID, String mimeType, int testJobId) throws InterruptedScanException;
}
