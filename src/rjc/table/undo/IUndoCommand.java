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

package rjc.table.undo;

/*************************************************************************************************/
/**************************** Interface for all commands on UndoStack ****************************/
/*************************************************************************************************/

public interface IUndoCommand
{
  // applies the command
  public void redo();

  // reverts the command
  public void undo();

  // returns short text string describing this command, e.g. "insert text"
  public String text();

  // returns true if command valid and ready to be pushed onto undo-stack
  default public boolean isValid()
  {
    return true;
  }
}
