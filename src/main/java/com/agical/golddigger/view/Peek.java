package com.agical.golddigger.view;

import com.agical.golddigger.model.Position;
import com.agical.golddigger.model.Rectangle;
import com.agical.golddigger.model.tiles.Square;
import com.agical.golddigger.model.tiles.WallSquare;
import com.agical.jambda.Unit;
import com.agical.jambda.Functions.Fn1;
import static com.agical.jambda.Booleans.*;


public class Peek {

	private final Square[][] piece;
	private final Position position;
	private final Rectangle bounds;
	final static double hexXDistance = 3.0/1.85;	//Determines the x Distance between the tiles
	final static double hexYDistance = 2.0; 		//Determines the y Distance between the tiles.
	final static double hexR = 21.0;
	private int numberOfSides = 4;
	private int extraLeftWalls = 0;          //Used to offset the extra walls added to the left of the map when re-sizing the window
	final static double hexH = Math.sqrt(3.0)*hexR/2.0;	

	public Peek(Square[][] piece, Position position, Rectangle bounds) {
		this.piece = piece;
		this.position = position;
		this.bounds = bounds;
	}
	
	public Peek(Square[][] piece, Position position, Rectangle bounds, int numberOfSides, int extraLeftWalls) {
		this(piece, position, bounds);
		this.numberOfSides = numberOfSides;
		this.extraLeftWalls = extraLeftWalls;
	}
	
	public Rectangle getBounds() {
		return bounds;
	}
	
	public Position getPosition() {
		return position;
	}
	
	public Square[][] getPiece() {
		return piece;
	}

	public void drawTo(PeekView peekView) {
		for(int y = 0; y < piece.length; y++) {
			for(int x = 0; x < piece[y].length;x++) {
				drawSquare(x, y, peekView);
			}
		}
		diggerPosition(peekView);
	}

	private void diggerPosition(PeekView peekView) {
		int x = position.getLongitude() - bounds.getX1();
		int y = position.getLatitude() - bounds.getY1();
		if(numberOfSides == 6){
			if(extraLeftWalls > 0){
				y = hexY(x,y, bounds.getX1()+extraLeftWalls);
			} else {
				y = hexY(x,y, bounds.getX1());
			}
			x = hexX(x);
		}
		peekView.drawDigger(x,y);
	}

	private void drawSquare(int x, int y, PeekView peekView) {
		Square thisSquare = piece[y][x];
		String srep = thisSquare.getStringRepresentation();
		
		if(numberOfSides == 6){
			if(extraLeftWalls > 0){
				y = hexY(x,y, bounds.getX1()+extraLeftWalls);
			} else {
				y = hexY(x,y, bounds.getX1());
			}
			x = hexX(x);			
		}
		if(srep.equals(Square.wall().getStringRepresentation()))  {
			drawWall(x,y, peekView);
		}
		else if (srep.equals("b")) {
			peekView.drawBank(x, y);
		}
		else if (Character.isDigit(srep.charAt(0))) {
			char ch = srep.charAt(0);
			drawGold(x, y, ch-'0', peekView);
		} else if(srep.equals("c")){
			peekView.drawCity(x, y);
		} else if(srep.equals("d")){
			peekView.drawDeepWater(x, y);
		} else if(srep.equals("h")){
			peekView.drawHill(x, y);
		} else if(srep.equals("m")){
			peekView.drawMountain(x, y);
		} else if(srep.equals("r")){
			peekView.drawRoad(x, y);
		} else if(srep.equals("s")){
			peekView.drawShallowWater(x, y);
		} else if(srep.equals("t")){
			peekView.drawTeleport(x, y);
		} else if(srep.equals("f")){
			peekView.drawForest(x, y);
		} else {
			peekView.drawEmpty(x,y);
		}
		// orkade inte funktionalisera
		if(!thisSquare.hasBeenViewed().isSome()) {
		    peekView.drawShadow(x,y);
		}
	}

	private void drawGold(int x, int y, int i, PeekView peekView) {
		peekView.drawGold(x, y, i);
	}


	private void drawWall(int x, int y, PeekView peekView) {
		if(wallIs(x, y, SOLID))	peekView.drawSolidWall(x,y);
		else if(wallIs(x, y, SOUTHEAST_INV)) peekView.drawInvertedSouthEastWall(x,y);
		else if(wallIs(x, y, NORTHEAST_INV)) peekView.drawInvertedNorthEastWall(x,y);
		else if(wallIs(x, y, SOUTHWEST_INV)) peekView.drawInvertedSouthWestWall(x,y);
		else if(wallIs(x, y, NORTHWEST_INV)) peekView.drawInvertedNorthWestWall(x,y);
		else if(wallIs(x, y, NORTH)) peekView.drawNorthWall(x,y);
		else if(wallIs(x, y, NORTHEAST)) peekView.drawNorthEastWall(x,y);
		else if(wallIs(x, y, EAST))	peekView.drawEastWall(x,y);
		else if(wallIs(x, y, SOUTHEAST)) peekView.drawSouthEastWall(x,y);
		else if(wallIs(x, y, SOUTH)) peekView.drawSouthWall(x,y);
		else if(wallIs(x, y, SOUTHWEST)) peekView.drawSouthWestWall(x,y);
		else if(wallIs(x, y, WEST)) peekView.drawWestWall(x,y);
		else if(wallIs(x, y, NORTHWEST)) peekView.drawNorthWestWall(x,y);
		else peekView.drawCenterWall(x,y);
	}

	private boolean wallIs(int x, int y, Fn1<Square, Boolean>[][] pattern) {
		for (int dx = -1; dx <= 1; dx ++){
			for (int dy = -1; dy <= 1; dy ++){
				if (!pattern[dy+1][dx+1].apply(getSquare(x + dx, y + dy))) return false;
			}	
		}
		return true;
	}

	private Square getSquare(int x, int y) {
		if (x < 0 || x >= piece[0].length || y < 0 || y >= piece.length) return Square.wall();
		else return piece[y][x];
	}
	
	//offsets the X co-ordinates for 6 sided tiles.
	public static int hexX (double x){
		return (int) Math.round(hexXDistance * x * hexR);
	}
	
	//Offsets the Y co-ordinate for 6 sided tiles.
	public static int hexY (double x, double y, int c){
		if (c%2 == 0){
			return (int) Math.round((hexYDistance*y) * hexH + (x % 2) * hexH + hexH);
		} else {
			return (int) Math.round((hexYDistance*y) * hexH + ((x+1) % 2) * hexH);				
		}
	}
	
	private static Fn1<Square, Boolean> WALL = new Fn1<Square, Boolean>(){
		@Override
		public Boolean apply(Square square) {
			return square instanceof WallSquare;
		}
	};
	
	private static Fn1<Square, Boolean> ANY = new Fn1<Square, Boolean>(){
		@Override
		public Boolean apply(Square square) {
			return true;
		}
	};
	
	private static Fn1<Square, Boolean> NOTW = WALL.compose(not);
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] SOLID = new Fn1[][] {
		{WALL, WALL, WALL},
		{WALL, WALL, WALL},
		{WALL, WALL, WALL},
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] NORTH = new Fn1[][] {
		{ANY,  WALL, ANY },
		{WALL, WALL, WALL},
		{ANY,  NOTW, ANY },
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] NORTHEAST = new Fn1[][] {
		{WALL, WALL, WALL},
		{WALL, WALL, WALL},
		{NOTW,  WALL, WALL},
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] NORTHEAST_INV = new Fn1[][] {
		{ANY, NOTW, ANY},
		{WALL, WALL, NOTW},
		{WALL, WALL, ANY},
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] EAST = new Fn1[][] {
		{ANY,  WALL, ANY },
		{NOTW, WALL, WALL},
		{ANY,  WALL, ANY },
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] SOUTHEAST = new Fn1[][] {
		{NOTW, WALL, WALL},
		{WALL, WALL, WALL},
		{WALL, WALL, WALL},
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] SOUTHEAST_INV = new Fn1[][] {
		{WALL, WALL, ANY},
		{WALL, WALL, NOTW},
		{ANY, NOTW, ANY },
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] SOUTH = new Fn1[][] {
		{ANY,  NOTW, ANY},
		{WALL, WALL, WALL},
		{ANY,  WALL, ANY},
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] SOUTHWEST = new Fn1[][] {
		{WALL, WALL, NOTW},
		{WALL, WALL, WALL},
		{WALL, WALL, WALL},
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] SOUTHWEST_INV = new Fn1[][] {
		{ANY, WALL, WALL },
		{NOTW, WALL, WALL },
		{ANY , NOTW, ANY },
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] WEST = new Fn1[][] {
		{ANY,  WALL, ANY },
		{WALL, WALL, NOTW},
		{ANY,  WALL, ANY },
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] NORTHWEST = new Fn1[][] {
		{WALL, WALL, WALL },
		{WALL, WALL, WALL },
		{WALL, WALL, NOTW },
	};
	
	@SuppressWarnings("unchecked")
	private static Fn1<Square, Boolean>[][] NORTHWEST_INV = new Fn1[][] {
		{ANY , NOTW, ANY },
		{NOTW, WALL, WALL },
		{ANY, WALL, WALL },
	};
}
