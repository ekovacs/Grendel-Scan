package com.grendelscan.tests.testTypes;

import com.grendelscan.scan.InterruptedScanException;
import com.grendelscan.tests.libraries.SessionIDTesting.SessionIDLocation;

/**
 * 
 * @author David Byrne
 */
public interface InitialAuthenticationTest extends TestType
{
	public void testInitialAuthentication(int transactionID, SessionIDLocation sessionIDLocation, int testJobId) throws InterruptedScanException;
}
