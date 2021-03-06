package com.grendelscan.proxy;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericProxyRequestListenerThread extends Thread
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericProxyRequestListenerThread.class);
	protected final HttpParams params;
	protected AbstractProxy proxy;
	protected final ServerSocket serverSocket;
	
	public GenericProxyRequestListenerThread(ServerSocket serverSocket, AbstractProxy proxy)
	{
		this.proxy = proxy;
		this.serverSocket = serverSocket;
		params = MiscHttpFactory.createDefaultHttpProxyParams();
		setDaemon(true);
	}
	
	public void shutdown()
	{
        this.interrupt();
	}
	
	@Override
	public void run()
	{
		Socket socket;
		LOGGER.debug("Listening on port " + serverSocket.getLocalPort());
		while (!Thread.interrupted() && proxy.isRunning())
		{
			synchronized (serverSocket)
			{
				try
				{
					socket = serverSocket.accept();
				}
				catch (IOException e)
				{
//					Debug.errDebug("Problem with accepting a proxy socket: " + e.toString(), e);
					continue;
				}
			}
			if (proxy.isRunning())
			{
				handleNewConnection(socket);
			}
		}
	}

	
    protected void handleNewConnection(Socket socket)
	{
		DefaultHttpProxyConnection httpProxyConnection = null;
		try
		{
			if (!proxy.isRunning())
			{
				return;
			}
			// Set up HTTP connection
			httpProxyConnection = new DefaultHttpProxyConnection();
			LOGGER.debug("Incoming connection from " + socket.getInetAddress());
			httpProxyConnection.bind(socket, params);
			
			HttpService httpService = MiscHttpFactory.createHttpServiceProxy(proxy.getRequestHandler(socket, false, -1));
			
			if (!proxy.isRunning())
			{
				return;
			}
			
			HttpContext context = new BasicHttpContext(null);
			while (!Thread.interrupted() && httpProxyConnection.isOpen() && proxy.isRunning())
			{
				httpService.handleRequest(httpProxyConnection, context);
			}
		}
		catch (SocketTimeoutException e)
		{
			//just means that the connection is done (I think)
		}
		catch (SocketException e)
		{
			// ignore?
		}
		catch (InterruptedIOException ex)
		{
			LOGGER.error("Proxy interupted: " + ex.toString(), ex);
			return;
		}
		catch (ConnectionClosedException ex)
		{
//			Debug.errDebug("Client closed connection", ex);
		}
		catch (HttpException ex)
		{
			LOGGER.error("Unrecoverable HTTP protocol violation: " + ex.getMessage(), ex);
		}
		catch (IOException e)
		{
			LOGGER.error("I/O error initializing connection thread: " + e.getMessage(), e);
			return;
		}
		catch (Exception e)
		{
			LOGGER.error("Big problem in proxy WorkerThread: " + e.toString(), e);
		}
		finally
		{
			try
			{
				if (httpProxyConnection != null)
					httpProxyConnection.shutdown();
			}
			catch (IOException ignore)
			{
				// ignore
			}
		}
	}
}
