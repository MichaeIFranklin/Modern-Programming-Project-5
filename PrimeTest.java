//********************************************************************
//
//  Developer:           Instructor
//
//  Project #:           Five
//
//  File Name:           PrimeTest.java
//
//  Course:              COSC 4301 - Modern Programming
//
//  Due Date:            3/13/2022 
//
//  Instructor:          Fred Kumi 
//
//  Description:         PrimeTest class
//
//********************************************************************

import java.util.Scanner;

public class PrimeTest
{
   //***************************************************************
   //
   //  Method:       main
   // 
   //  Description:  The main method of the program
   //
   //  Parameters:   String array
   //
   //  Returns:      N/A 
   //
   //**************************************************************
   public static void main(String[] args) 
   {
	  PrimeTest obj = new PrimeTest();
      Scanner input = new Scanner(System.in);

      System.out.print("\nEnter an integer: "); 
      int number = input.nextInt(); 
	  
	  if (obj.isPrime(number))
         System.out.println("\n" + number + " is a prime number");
      else
         System.out.println("\n" + number + " is not a prime number");
	  
	  input.close();
   }

   //***************************************************************
   //
   //  Method:       isPrime (Non Static)
   // 
   //  Description:  This method determines whether a positive integer is
   //                a prime number.  It returns true if the integer a prime
   //                number, and false if it is not.
   //
   //  Parameters:   A Positive Integer
   //
   //  Returns:      boolean
   //
   //**************************************************************
   public boolean isPrime(int number)
   {  
	  boolean rtnValue = true;
	  
      if (number < 2)            // Integers < 2 cannot be prime
         rtnValue = false;
      else if (number == 2)      // Special case: 2 is the only even prime number
         rtnValue = true;
      else if (number % 2 == 0)  // Other even numbers are not prime
         rtnValue = false;
      else {
         // Test odd divisors up to the square root of number
         // If any of them divide evenly into it, then number is not prime
         for (int divisor = 3; divisor <= Math.sqrt(number); divisor += 2)
         {
		     if (number % divisor == 0)
                rtnValue = false;
         }
      }
      
      return rtnValue;
   }
}

