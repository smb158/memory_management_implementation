//Steven Bauer
//Project 3
//March 17th 2012

import java.io.*;
import java.util.*;

public class project3 
{
	
	public static <E> void main(String[] args)
	{
		//check arguments
		if(args.length == 0)
		{
			System.out.println("Command Line arguments required.");
			System.out.println("./vmsim –n <numframes> -a <opt|clock|nru> [-r <NRUrefresh>] <tracefile>");
			System.exit(0);
		}
		
		memoryList memList = new memoryList();
		
		//Stats
		int frames = 0;
		int memaccess = 0;
		int pgfaults = 0;
		int diskwrites = 0;
		
		//other variables
		String mode = null;
		String tracefile;
		int NRUrefresh = 0;
		int alreadyHere = 0;
		
		//parse commandline arguments
		for(int x = 0; x < args.length; x++)
		{
			if(args[x].equals("-n"))
			{
				frames = Integer.parseInt(args[x+1]);
			}
			else if(args[x].equals("-a"))
			{
				mode = args[x+1];
				System.out.println(mode);
				if(mode.equals("nru"))
				{
					if(args[x] == "-r")
					{
						NRUrefresh = Integer.parseInt(args[x+1]);
					}
				}
			}
		}
		
		tracefile = args[args.length-1];
		
		if(mode.equals("opt"))
		{
			//copy all memory addresses into our future table for opt
			try
			{
				//setup our file reader and initialize some variables
				FileReader in = new FileReader(tracefile);
				BufferedReader bufRead = new BufferedReader(in);
			
				String line;
				int count = 0;
				String thisAddy = "";
				char thisMode = ' ';
			
				line = bufRead.readLine();
				count++;
			
				while(line != null)
				{
					StringTokenizer st = new StringTokenizer(line);
					while(st.hasMoreTokens())
					{
						thisAddy = st.nextToken();
						thisMode = st.nextToken().charAt(0);
					}
				
					//add to data structure
				
					System.out.println(thisAddy);
					memList.addAddress(thisAddy, thisMode, count);
				
					//continue reading the rest of the trace file
					line = bufRead.readLine();
					count++;
				}
			
				//we're done reading in the memory accesses so lets close the file
				bufRead.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		LinkedList pgTable = null;
		LinkedList nruTable = null;
		LinkedList clockList = null;
		Iterator clockHead = null;
		
		if(mode.equals("opt"))
		{
			//evict page that won't be needed until furthest in the future
			
			//create our page table with requested number of frames
			pgTable = new LinkedList();
		}
		else if(mode.equals("nru"))
		{
			//evict page that is the oldest, preferring pages that are not dirty
			
			//create our page table with requested number of frames
			nruTable = new LinkedList();
		}
		else if(mode.equals("clock"))
		{
			//circular queue improvement of second chance algorithm
			clockList = new LinkedList();
			clockHead = clockList.iterator();
		}
				
		try
		{
			//setup our file reader and initialize some variables
			FileReader in = new FileReader(tracefile);
			BufferedReader bufRead = new BufferedReader(in);
			
			String line;
			int count = 0;
			String thisAddy = "";
			char thisMode = (Character) null;
			
			line = bufRead.readLine();
			count++;
			
			while(line != null)
			{
				StringTokenizer st = new StringTokenizer(line);
				thisAddy = st.nextToken();
				thisMode = st.nextToken().charAt(0);
				
				String[] data = null;
				
				if(mode.equals("opt"))
				{
					//create data array for pg table
					data = new String[2];
					data[0] = thisAddy;
					data[1] = Character.toString(thisMode);
				}
				else if(mode.equals("nru") ||mode.equals("clock"))
				{
					//create data array for pg table
					data = new String[3];
					data[0] = thisAddy;
					data[1] = Character.toString(thisMode);
					//reference bit
					data[2] = "1";
				}
				//count it as a memory access
				memaccess++;
					
				if(mode.equals("opt"))
				{
					//add to Page Table
					Iterator iterate = pgTable.listIterator();
					
					while(iterate.hasNext())
					{
						String[] tempData = (String[]) iterate.next();
					
						//check if entry is already in page table
						if(tempData[0] == data[0])
						{
							//entry already in page table. lets check if its a write and if so update the entry
							if(tempData[1].equals("R") && data[1].equals("W"))
							{
								tempData[1] = "W";
							}
							alreadyHere = 1;
						}
					}
					
					if(alreadyHere == 0)
					{
						if(pgTable.size() == frames)
						{	
							//time to evict something
							optEviction(pgTable,memList,diskwrites);
						}
				
						if(pgTable.size() < frames)
						{
							//well its not in the page table so lets add it
							//add new pages data
							pgTable.add(data);
							//count that page fault has occured
							pgfaults++;
						}
					}
				}
				else if(mode.equals("nru"))
				{
					//set all reference bits to 0 at user set interval
					if (count % NRUrefresh == 0)
					{
						Iterator allUnref = nruTable.iterator();
						while(allUnref.hasNext())
						{
							String[] unref = (String[]) allUnref.next();
							unref[2] = "0";
						}
					}
					
					//add to Page Table
					Iterator iterate = nruTable.iterator();
					while(iterate.hasNext())
					{
						String[] tempData = (String[]) iterate.next();
						//check if entry is already in page table
						if(tempData[0] == data[0])
						{
							//entry already in page table. lets check if its a write and if so update the entry
							if(tempData[1].equals("R") && data[1].equals("W"))
							{
								tempData[1] = "W";
							}
							alreadyHere = 1;
						}
					}
					
					if(alreadyHere == 0)
					{
						if(nruTable.size() == frames)
						{
							//time to evict something
							nruEviction(nruTable, data, diskwrites);
						}
					
						if(nruTable.size() < frames)
						{
							iterate = nruTable.iterator();
						
							while(iterate.hasNext())
							{
								String[] tempData = (String[]) iterate.next();
								
								//check if entry is already in page table
								if(tempData[0] == data[0])
								{
									//entry already in page table. lets check if its a write and if so update the entry
									if(tempData[1].equals("R") && data[1].equals("W"))
									{
										tempData[1] = "W";
									}
								}
								//well its not in the page table so lets add it
								else
								{
									//add new pages data
									nruTable.add(data);
									//count that page fault has occured
									pgfaults++;
								}
							}
						}	
					
					}
				}
				else if(mode.equals("clock"))
				{					
					Iterator clockIterate = clockList.iterator();
					while(clockIterate.hasNext())
					{
						String[] tempData = (String[]) clockIterate.next();
						//check if entry is already in page table
						if(tempData[0] == data[0])
						{
							//entry already in page table. lets check if its a write and if so update the entry
							if(tempData[1].equals("R") && data[1].equals("W"))
							{
								tempData[1] = "W";
							}
							alreadyHere = 1;
						}
					}
					
					if(alreadyHere == 0)
					{
						if(clockList.size() < frames)
						{
							clockList.add(data);
						}
						else
						{
							//page table is full, lets run the clock eviction algorithm
							clockEviction(clockHead, data, diskwrites);
						}
					}
				
					//continue reading the rest of the trace file
					line = bufRead.readLine();
					count++;
				}
				}
				//we're done reading in the memory accesses so lets close the file
				bufRead.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		
		// resulting output
		System.out.println("Number of frames:\t\t"+frames);
		System.out.println("Total memory accesses:  "+memaccess);
		System.out.println("Total page faults:\t"+pgfaults);
		System.out.println("Total writes to disk:\t"+diskwrites);
	}
	
	public static void optEviction(LinkedList pgTable,memoryList memList,int diskwrites)
	{
		//returns the index of the page to evict from pageTable.
		int farthestFutureTime = 0;
		int addressWithFarthestFutureTimePos = -1;
		
		int tempTime;
		
		for(int x = 0; x < pgTable.size(); x++)
		{
			//get next use of current address
			String[] tempData = (String[]) pgTable.get(x);
			tempTime = memList.getNextUse(tempData[0]);
			
			if(tempTime == -1)
			{
				//this one is never used again so we should evict it.
				addressWithFarthestFutureTimePos = x;
				break;
				
			}
			else if(tempTime > farthestFutureTime)
			{
				//great! so far this is our best eviction choice
				farthestFutureTime = tempTime;
				addressWithFarthestFutureTimePos = x;
			}
		}
		
		//check if dirty and if to write to disk
		String[] tempData = (String[]) pgTable.get(addressWithFarthestFutureTimePos);
		if(tempData[1].equals("W"))
		{
			diskwrites++;
		}
		
		//finally remove the entry
		pgTable.remove(addressWithFarthestFutureTimePos);
	}
	public static void clockEviction(Iterator clockHead, String[] newData,int diskwrites)
	{
		//clock eviction algorithm code
		while(true)
		{
			String[] tempData = (String[]) clockHead.next();
			
			if(tempData[2].equals("1"))
			{
				//currently referenced so lets set it to unreferenced and move the head
				tempData[2] = "0";
			}
			else
			{
				//unreferenced so lets evict it and write to disk if dirty
				if(tempData[1].equals("W"))
				{
					diskwrites++;
				}
				
				//replace with new page
				tempData[0] = newData[0];
				tempData[1] = newData[1];
				tempData[2] = newData[2];
				
				break;
			}
		}
	}
	
	public static void nruEviction(LinkedList nruTable, String[] newData, int diskwrites)
	{
		/* candidate scoring
		 * NRU prefers to evict an unreferenced page and its second criteria is if its dirty or clean
		 * my plan is to "score" candidates based on this system and the candidate with the lowest (best)
		 * score will get evicted.
		 * 
		 * unref     dirty   score
		 * 0         0       0
		 * 0         1       1
		 * 1         0       2
		 * 1         1       3
		 */

		
		int evictCandidateScore;
		//position of candidate
		Iterator candidate = null;
		// "score" of candidate
		evictCandidateScore = 0;
		
		/*
		data[0] = thisAddy;
		data[1] = Character.toString(thisMode);
		//reference bit
		data[2] = "1";
		*/
		
		Iterator nruIterator = nruTable.iterator();
		while(nruIterator.hasNext())
		{
			String[] tempData = (String[]) nruIterator.next();
			
			if(tempData[2].equals("0") && tempData[1].equals("R"))
			{
				evictCandidateScore = 0;
				candidate = nruIterator;
			}
			else if(tempData[2].equals("0") && tempData[1].equals("W"))
			{
				evictCandidateScore = 1;
				candidate = nruIterator;
			}
			else if(tempData[2].equals("1") && tempData[1].equals("R"))
			{
				evictCandidateScore = 2;
				candidate = nruIterator;
			}
			else
			{
				evictCandidateScore = 3;
				candidate = nruIterator;
			}
		}
		
		//remove the entry and write to disk if dirty
		if(evictCandidateScore == 1 || evictCandidateScore == 3)
		{
			diskwrites++;
		}
		
		candidate.remove();
	}
}
//The End :)
