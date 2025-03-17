/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

package rjc.table.undo;

import javafx.scene.Scene;
import javafx.stage.Stage;

/*************************************************************************************************/
/****************************** Window for undo-stack command list *******************************/
/*************************************************************************************************/

public class UndoStackWindow extends Stage
{

  /**************************************** constructor ******************************************/
  public UndoStackWindow( UndoStack undoStack )
  {
    // create undo-stack window
    setTitle( "Undostack" );
    setWidth( 250 );
    setHeight( 300 );

    // setup scene
    UndoStackView control = new UndoStackView( undoStack );
    Scene scene = new Scene( control );
    setScene( scene );
  }

}