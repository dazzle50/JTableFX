/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
 *  https://github.com/dazzle50/JTableFX                                  *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.table.demo;

import java.util.TreeSet;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import rjc.table.Utils;

/*************************************************************************************************/
/******************* Application to demonstrate JTableFX use and capabilities ********************/
/*************************************************************************************************/

public class Demo extends Application
{
  /******************************************** main *********************************************/
  public static void main( String[] args )
  {
    // entry point for demo application startup
    Utils.trace( "################################# Java properties #################################" );
    for ( Object property : new TreeSet<Object>( System.getProperties().keySet() ) )
      Utils.trace( property + " = '" + System.getProperty( property.toString() ) + "'" );

    Utils.trace( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ JTableFX  demo started ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
    Utils.trace( "JTableFX  VERSION = '" + Utils.VERSION + "'", args );

    // launch demo application display
    launch( args );
    Utils.trace( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ JTableFX  demo ended ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
  }

  /******************************************** start ********************************************/
  @Override
  public void start( Stage primaryStage ) throws Exception
  {
    // create demo application window
    Scene scene = new Scene( new GridPane() );
    primaryStage.setScene( scene );

    // close demo app when main window is closed (in case other windows are open)
    primaryStage.setOnHidden( event -> Platform.exit() );

    // open demo app window
    primaryStage.setWidth( 600 );
    primaryStage.setHeight( 400 );
    primaryStage.show();
  }

}
