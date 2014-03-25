import java.util.*;

public class memoryList {
	
	LinkedList[] memoryArray;
	
	public memoryList()
	{
		memoryArray = new LinkedList[1048576];
	}
	public void addAddress(String address, char rw, int timeused)
	{
		String[] nodeData = new String[2];
		//set read/write
		nodeData[0] = Character.toString(rw);
		//set time
		nodeData[1] = Integer.toString(timeused);
		
		//turn the hex address into a decimal value
		int parsedHex = Integer.parseInt(address,16);
		parsedHex >>= 12;
		
		//System.out.println(parsedHex);
		//System.out.println(nodeData[0]+nodeData[1]);
		
		
		//add to the datatype
		memoryArray[parsedHex].add(nodeData);
	}
	public int getNextUse(String address)
	{
		int timeUsed = -2;
		
		//returns the next time that address is used
		
		//shift off bottom 12 of hex address
		String newAddress = address.substring(11);
		//turn the hex address into a decimal value
		Integer parsedHex = Integer.parseInt(newAddress,16);
		
		//get next time used
		LinkedList temp = memoryArray[parsedHex];
		
		if(temp.isEmpty())
		{
			//never used again. best case.
			timeUsed = -1;
		}
		
		String[] tempdata = (String[]) memoryArray[parsedHex].getFirst();
		timeUsed = Integer.parseInt(tempdata[1]);
		
		if(timeUsed == -2)
		{
			System.out.println("Something has gone wrong.");
		}
		
		return timeUsed;
	}

}
