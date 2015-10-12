package de.uni_hannover.sra.minimax_simulator.ui;

import static com.google.common.base.Preconditions.*;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Throwables;
import javafx.application.Platform;

/**
 *
 * @author Philipp Rohde
 * @author Martin L&uuml;ck
 */
public class UI
{
	private static UI	instance;

	/**
	 * Initializes the UI of the application.
	 */
	public static void initialize() {
		assert (instance == null);
		instance = new UI();
	}

//	public static UI getInstance()
//	{
//		assert (instance != null);
//		return instance;
//	}

	private UI() {

	}

	/**
	 * Schedules asynchronous execution of the {@link Runnable} in the FX application thread.
	 * 
	 * @param r
	 * 			the {@code Runnable} to schedule for asynchronous execution
	 */
	// TODO: delete; this is basically the same like invokeInFAT
	public static void invoke(Runnable r) {
		checkNotNull(r);
		Platform.runLater(r);
	}

	/**
	 * If called from the FX application thread, just calls {@link Runnable#run()} on <code>r</code>.<br>
	 * Otherwise, schedules its execution there.
	 *
	 * @param r
	 * 			the {@code Runnable} to execute in FX application thread
	 */
	public static void invokeInFAT(Runnable r) {
		checkNotNull(r);
		if (Platform.isFxApplicationThread()) {
			r.run();
		}
		else {
			Platform.runLater(r);
			//invokeNowFX(r);
		}
	}

	/**
	 * Schedule execution of the {@link Runnable} in the Swing event dispatch thread and blocks
	 * until the call succeeded or an exception or error occurs during the execution. <br>
	 * In the second case, this method will throw the occured {@link RuntimeException} or wrap the
	 * occured checked exception into one.
	 * 
	 * @param r
	 */
	// This would work if invokeAndWait() would.
	public static void invokeNowFX(Runnable r) {
		checkNotNull(r);
		try {
			System.out.println("I am trying to invokeAndWait()");
			invokeAndWait(r);
		}
		catch (ExecutionException e) {
			System.out.println("Error: ExecutionException");
			throw Throwables.propagate(e.getCause());
		}
		catch (InterruptedException e) {
			System.out.println("Error: InterruptedException");
			throw new Error("Invoking thread interrupted while waiting: " + Thread.currentThread().getName(), e);
		}
	}

	// According to https://community.oracle.com/thread/2372263 this should do the trick
	// but the runnable is not executed before task.get() finishes. This is the reason for the timeout.
	private static void invokeAndWait(Runnable r) throws InterruptedException, ExecutionException {
		checkNotNull(r);
//		FutureTask<Boolean> task = new FutureTask<>(r, true);
		FutureTask<Boolean> task = new FutureTask<>(new Runnable() {
			@Override
			public void run() {
				// Do something on FX thread
				System.out.println("NOW IT IS WORKING!");
			}
		}, true);

		System.out.println("Call: runLater()");
		Platform.runLater(task);
//		System.out.println("Call: task.get()");
//		task.get();
//		System.out.println("finished waiting");
		try {
			task.get(10, TimeUnit.SECONDS);
		}
		catch (TimeoutException e) {
			// timed out
			return;
		}
	}

}