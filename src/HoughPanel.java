import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JPanel;

public class HoughPanel extends JPanel
{
	BufferedImage sourceImg,edgeImg, houghImg, resultImg;
	ArrayList<Circle> foundCircles;
	public HoughPanel()
	{
		super();
		int[][][] RGBSource = ImageManager.RGBArrayFromFile("dots&cards.jpg");
		sourceImg = ImageManager.ImageFromArray(RGBSource);
		
		int[][] grayEdges = findEdges(ImageManager.toGrayArray(RGBSource));
		edgeImg = ImageManager.ImageFromArray(grayEdges);
		
		int[][] houghArray = generateHough(grayEdges);
		// Note: houghArray is not an image array - it has numbers that might go much 
		//       higher than 255 (or it might be much lower). We need to multiply it 
		//       by a factor so that the just the largest number is 255 before we can
		//       display that. (We'll keep the houghArray the same and make a modified 
		// 		 copy.)
		//       This is called "normalizing" the array.
		houghImg = ImageManager.ImageFromArray(normalizeArrayTo255(houghArray));
		
		foundCircles = findBestCircles(houghArray);
		
		int[][][] resultArray = buildResult(RGBSource,foundCircles);
		resultImg = ImageManager.ImageFromArray(resultArray);
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		int w = sourceImg.getWidth();
		int h = sourceImg.getHeight();
		g.drawImage(sourceImg, 0, 0, null);
		g.drawImage(edgeImg, w, 0, null);
		g.drawImage(houghImg, 0, h, null);
		g.drawImage(resultImg, w, h, null);
		
		g.setColor(Color.BLACK);
		g.drawString("Original", 3, 12);
		g.drawString("Edges", w+3, 12);
		g.drawString("Hough Map", 3, 12+h);
		g.drawString("Result", w+3, h+12);
		
		g.setColor(Color.WHITE);
		g.drawString("Original", 2, 11);
		g.drawString("Edges", w+2, 11);
		g.drawString("Hough Map", 2, 11+h);
		g.drawString("Result", w+2, h+11);
		
		for (Circle c: foundCircles)
		{
			g.setColor(Color.RED);
			g.drawOval(w+c.getCenterX()-27,h+c.getCenterY()-27,54,54);
		}
	}
	
	/**
	 * measure the difference (delta) between each pixel and its neighbors; return an array
	 * of size 2 pixel smaller in each direction as the original with 0 for pixels with small delta (below some 
	 * threshold) and 255 for pixels with large delta (at or above the threshold).
	 * @param sourceArray
	 * @return - an array one pixel shorter on each edge, with either 0 or 255 values indicating whether there
	 *           were large changes in the intensity of each pixel, compared to the intensity
	 *           of the neighboring pixels.
	 */
	public int[][] findEdges(int[][] sourceArray)
	{
		double deltaSquaredThreshold = 800; // if dx^2 + dy^2 > threshold, we'll call it an edge.
		
		int[][] edgeArray = ImageManager.createGrayscaleArrayOfSize(sourceArray.length-1, 
																	sourceArray[0].length-1);
		// TODO: insert your code here. DONE
		for(int x=0;x<sourceArray.length-1;x++){
			for(int y=0;y<sourceArray[0].length-1;y++){
				if(Math.pow(sourceArray[x][y]-sourceArray[x+1][y],2)+Math.pow(sourceArray[x][y]-sourceArray[x][y+1], 2)>deltaSquaredThreshold){
					edgeArray[x][y] = 255;
				}
				else{
					edgeArray[x][y] = 0;
				}
			}
		}
		//-------------------------------------------------
		return edgeArray;
	}
	/**
	 * for every "high"(255) pixel in the grayEdges matrix, consider all the pixels that might
	 * have had a circle centered on them that might have had an edge at the 255 pixel, and 
	 * "upvote" those candidate pixels in the the hough array. (The hough array is the same size
	 * as the grayEges array and should start off at zero in all places.
	 * @param grayEdges - an array of just 0 or 255 values (nothing in the middle)
	 * @return hough array - an identically sized array that contains "votes" for locations where
	 * there might have been circles that caused the pixels found in the gray edges. 
	 */
	public int[][] generateHough(int[][] grayEdges)
	{
	    int TARGET_RADIUS = 27;
	    
		int[][] houghArray = new int[grayEdges.length][ grayEdges[0].length];
		
	    // TODO: Insert your code here.
	    // I recommend that you do the "normalizeArrayTo255()" TO-DO before you do this one so you 
		//   can see your results.
		for(int x=0;x<houghArray.length;x++){
			for(int y=0;y<houghArray[0].length;y++){
				if (grayEdges[x][y] == 255){
					for(int xx=x-TARGET_RADIUS;xx<x+TARGET_RADIUS;xx++){
						for(int yy=y-TARGET_RADIUS;yy<y+TARGET_RADIUS;yy++){
							if(xx>-1&&xx<houghArray.length&&yy>-1&&yy<houghArray[0].length){
								int distance = (int)Math.sqrt(Math.pow((x-xx), 2)+Math.pow((y-yy), 2));
								if (distance == TARGET_RADIUS){
									houghArray[xx][yy] += 1;
								}
							}
						}
					}
				}
			}
		}
	    // temp code:
	    //houghArray[grayEdges.length/2][grayEdges[0].length/2] += 1; // one sample upvote. Remove this line in your program.
	    // ----------------------------
		return houghArray;
	}
	
	/**
	 * finds the highest several vote-getters in the houghArray.
	 * @param RGBSource - the original picture
	 * @param houghArray - the votes for where there are circles centered.
	 * @return a copy of the original picture, with red circles superimposed.
	 */
	public ArrayList<Circle> findBestCircles(int [][] houghArray)
	{
		int maxNumCircles = 8; //the most circles you're hoping to get (adjust this)
		int votesThreshold = 60; // the minimum number of votes required to count as a "found" circle.
		int annihilationRadius = 10; // after you find a maximum in the hough array (and presumably do something with it),
        // wipe out all the votes within this radius of the vote winner to zero, so that
        // you are ready to get the next maximum. (Finding two maxima within a couple of pixels
        // is unlikely to be useful and more likely to be natural/rounding error.)
		int[][] houghCopy = ImageManager.deepCopyArray(houghArray);
		ArrayList<Circle> listOfCircles = new ArrayList<Circle>();
		
		
		/*
		 * 1) find the highest value in the Hough array & its location.
		 * 2) determine whether this is higher than votesThreshold. If so... 
		 *         •add this location to listOfCircles
		 *         •set all of the values in the Hough array within the annihilationRadius of this point to 0.
		 *    otherwise, stop looking in the Hough array.
		 * 3) keep looking until you no longer have high enough peaks or until you have found maxNumCircles.
		 */
		//TODO: Insert your code here.
		int counter = 0;
		int xLoc = 0;
		int yLoc = 0;
		while(counter<maxNumCircles){
			int max = 0;
			for (int x=0;x<houghArray.length;x++){
				for(int y=0;y<houghArray[0].length;y++){
					if(houghArray[x][y]>max){
						max = houghArray[x][y];
						xLoc = x;
						yLoc = y;
					}
				}
			}
			if(max>votesThreshold){
				listOfCircles.add(new Circle(yLoc,xLoc));
				counter ++;
				for(int xx=xLoc-annihilationRadius;xx<xLoc+annihilationRadius;xx++){
					for(int yy=yLoc-annihilationRadius;yy<yLoc+annihilationRadius;yy++){
						if(xx>-1&&xx<houghArray.length&&yy>-1&&yy<houghArray[0].length){
							int distance = (int)Math.sqrt(Math.pow((xLoc-xx), 2)+Math.pow((yLoc-yy), 2));
							if (distance <= annihilationRadius){
								houghArray[xx][yy] = 0;
							}
						}
					}
				}
			}	
		}
		//---------------------------
		return listOfCircles;
	}
	
	/**
	 * makes a copy of the RGBSource and inserts red dots at the centers of the circles on top of it.
	 * @param RGBSource - the original image to draw
	 * @param circleList - the locations of circles to draw on this picture
	 * @return a copy of the RGBSource image with the circles drawn in the appropriate locations.
	 */
	public int[][][] buildResult(int [][][] RGBSource, ArrayList<Circle> circleList)
	{
		int[][][] result = ImageManager.deepCopyArray(RGBSource);
		// for each location in the circle list, set the corresponding pixel in result to be red (255,0,0).
		// TODO: insert your code here.
		
		
		//------------
		return result;
	}
	/**
	 * multiplies every value in the unnormalized array by the same amount, so that the largest
	 * value in the resulting array is 255.
	 * @param unnormalized -a 2-D array of integers, not all of which are zero.
	 * @return - a 2-D array of integers of the same dimensions, proportional to the original array,
	 *           but with a maximum of 255.
	 * postcondition: the original, unnormalized array is unchanged.
	 */
	public int[][] normalizeArrayTo255(int[][] unnormalized)
	{
		int max =0;
		//TODO: Find the maximum value in the array.
		for(int x=0;x<unnormalized.length;x++){
			for(int y=0;y<unnormalized[0].length;y++){
				if(unnormalized[x][y]>max){
					max = unnormalized[x][y];
				}
			}
		}
		//-----------------------------------------
		if (max == 0)
			throw new RuntimeException("Could not normalize the array to 0 to 255; array was empty.");
		int [][] normalized = new int[unnormalized.length][unnormalized[0].length];
		//TODO: set the normalized to the original times 255 and divided by the max.
		// note: integer math makes a difference here. Get the right order.
		for(int x=0;x<unnormalized.length;x++){
			for(int y=0;y<unnormalized[0].length;y++){
				normalized[x][y] = (unnormalized[x][y]*255)/max;
			}
		}	
		//
		return normalized;
	}
}
