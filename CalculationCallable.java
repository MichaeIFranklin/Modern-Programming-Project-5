//********************************************************************
//
//  Developer:           Michael Franklin
//
//  Project #:           Five
//
//  File Name:           CalculationCallable.java
//
//  Course:              COSC 4301 - Modern Programming
//
//  Due Date:            3/13/2022 
//
//  Instructor:          Fred Kumi 
//
//  Description:        callable class that handles calculation tasks
//
//********************************************************************

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class CalculationCallable implements Callable<Double>
{
	private final int calcMode;
	
	private ArrayList<Integer> list;
	private double sum;
	private int count;
	private double mean;
	
	
	// sum constructor
	public CalculationCallable(ArrayList<Integer> list)
	{
		// Get Sum
		calcMode = 0;
		this.list = list;
	}
	// mean constructor
	public CalculationCallable(double sum, int count)
	{
		// get mean
		calcMode = 1;
		
		this.sum = sum;
		this.count = count;
	}
	// standard deviation constructor
	public CalculationCallable(double mean, ArrayList<Integer> list)
	{
		// get standard deviation
		calcMode = 2;
		this.mean = mean;
		this.list = list;
	}
	
	
   //***************************************************************
   //
   //  Method:       call
   // 
   //  Description:  The main method of the callable class
   //
   //  Parameters:   None
   //
   //  Returns:      N/A 
   //
   //**************************************************************
	@Override
	public Double call() throws Exception 
	{
		double output = 0;
		
		switch(calcMode)
		{
		case 0:
			// get sum
			output = GetSum();
			break;
		case 1:
			// get mean
			output = GetMean();
			break;
		case 2:
			// get standard deviation
			output = GetStDev();
			break;
		}
		
		return output;
	}
	
	
	//***************************************************************
	//
	//  Method:       GetSum
	// 
	//  Description:  gets the sum of the integers in the list
	//
	//  Parameters:   None
	//
	//  Returns:      double: the sum of the integers in the list 
	//
	//**************************************************************
	public double GetSum()
	{
		double sum = 0;

		synchronized(list)
		{
			for (int i = 0; i < list.size(); i++)
			{
				sum += list.get(i);
			}
		}

		return sum;
	}
	
  
	//***************************************************************
	//
	//  Method:       GetMean
	// 
	//  Description:  gets the mean of the integers in the list
	//
	//  Parameters:   None
	//
	//  Returns:      double: the mean of the integers in the list 
	//
	//**************************************************************
	public double GetMean()
	{		
		return sum / count;
	}
	
	
	//***************************************************************
	//
	//  Method:       GetStDev
	// 
	//  Description:  gets the standard deviation of the integers in the 
	//				  list
	//
	//  Parameters:   None
	//
	//  Returns:      double: the standard deviation of the integers in the 
	//				  list 
	//
	//**************************************************************
	public double GetStDev()
	{
		double stdev = 0;

		// for each integer
		synchronized(list)
		{
			for (int i = 0; i < list.size(); i++)
			{
				// get the difference between current integer and mean
				double diff = list.get(i) - mean;
				
				// square that
				diff = Math.pow(diff,2);
				
				// add the result to stdev
				stdev += diff;	
			}
			
			// divide stdev by the total integers
			stdev /= (double)list.size();
		}
		// get the square root of the result to finalize the calculation
		stdev = Math.sqrt(stdev);

		
		return stdev;
	}
}

