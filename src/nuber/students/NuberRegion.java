package nuber.students;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Queue;
/**
 * A single Nuber region that operates independently of other regions, other than getting 
 * drivers from bookings from the central dispatch.
 * 
 * A region has a maxSimultaneousJobs setting that defines the maximum number of bookings 
 * that can be active with a driver at any time. For passengers booked that exceed that 
 * active count, the booking is accepted, but must wait until a position is available, and 
 * a driver is available.
 * 
 * Bookings do NOT have to be completed in FIFO order.
 * 
 * @author james
 *
 */
public class NuberRegion {

	public String regionName;
	public Queue<Callable<BookingResult>> waitingJobs;
	public Queue<Callable<BookingResult>> simultaneousJobs;
	public boolean isShutdown;

	private NuberDispatch dispatch;
	private int maxSimultaneousJobs;
	
	/**
	 * Creates a new Nuber region
	 * 
	 * @param dispatch The central dispatch to use for obtaining drivers, and logging events
	 * @param regionName The regions name, unique for the dispatch instance
	 * @param maxSimultaneousJobs The maximum number of simultaneous bookings the region is allowed to process
	 */
	public NuberRegion(NuberDispatch dispatch, String regionName, int maxSimultaneousJobs)
	{
		this.dispatch = dispatch;
		this.regionName = regionName;
		this.waitingJobs = new ConcurrentLinkedQueue<Callable<BookingResult>>();
		this.simultaneousJobs = new ConcurrentLinkedQueue<Callable<BookingResult>>();
		this.maxSimultaneousJobs = maxSimultaneousJobs;
		this.isShutdown = false;
	}
	
	/**
	 * Creates a booking for given passenger, and adds the booking to the 
	 * collection of jobs to process. Once the region has a position available, and a driver is available, 
	 * the booking should commence automatically. 
	 * 
	 * If the region has been told to shutdown, this function should return null, and log a message to the 
	 * console that the booking was rejected.
	 * 
	 * @param waitingPassenger
	 * @return a Future that will `provide the final BookingResult object from the completed booking
	 */
	public Future<BookingResult> bookPassenger(Passenger waitingPassenger)
	{	
		if (!isShutdown && simultaneousJobs.size() <= maxSimultaneousJobs){
			Callable<BookingResult> booking = new Booking(dispatch, waitingPassenger);
			simultaneousJobs.add(booking);
			waitingJobs.add(booking);
			ExecutorService service = Executors.newSingleThreadExecutor();
			Future<BookingResult> future = service.submit(booking);
			return future;
		}
		if (isShutdown){
			dispatch.logEvent(null, "Failed to make booking as region is Shutdown.");
			return null;
		}
		dispatch.logEvent(null, "Exceeded max simultaneous bookings.");
			return null;
	}
	
	/**
	 * Called by dispatch to tell the region to complete its existing bookings and stop accepting any new bookings
	 */
	public void shutdown()
	{
		this.isShutdown = true;
	}
}
