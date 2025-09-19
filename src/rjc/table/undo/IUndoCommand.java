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

/*************************************************************************************************/
/**************************** Interface for all commands on UndoStack ****************************/
/*************************************************************************************************/

/**
 * Interface defining the contract for all undoable commands that can be executed and reversed
 * within the JTableFX undo/redo system. 
 * 
 * <p>Each command encapsulates both the forward operation (redo) and its
 * inverse operation (undo), maintaining the necessary state to support both directions. 
 * Commands are typically pushed onto an {@code UndoStack} where they can be 
 * undone, and redone as needed. The stack maintains the history of operations and manages
 * the current position within that history.</p>
 * 
 * <p><strong>Constructor Pattern:</strong> Command constructors normally attempt to perform
 * the required action of the command during construction. This approach serves multiple purposes:
 * testing parameter validity, verifying system state compatibility, and confirming that the
 * desired change would result from the operation. This pre-execution enables the
 * {@link #isValid()} method to provide accurate indication based on the constructor's
 * success or failure.</p>
 */

public interface IUndoCommand
{
  /**
   * Re-executes the command's forward operation after an undo
   */
  public void redo();

  /**
   * Reverts the command's operation, restoring the system to its previous state
   */
  public void undo();

  /**
   * Returns concise human-readable description of this command, e.g. "Insert text"
   */
  public String text();

  /**
   * Returns true if command ready and valid to be pushed onto undo-stack
   */
  default public boolean isValid()
  {
    return true;
  }
}
