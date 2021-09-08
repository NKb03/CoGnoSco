# CoGnoSco

**CoGnoSco** is a user interface to **Co**nvert **G**raphic **n**otations to **o**rdinary **Sco**res.

### What it does

When you start CoGnoSco it will show a new graphic score.  
![Window showing new score](readme/new_score_annotated.png)
Creating and editing scores should be almost self-explanatory.  

#### Global keyboard shortcuts

These shortcuts trigger global actions independent of the currently selected element.

| Shortcut | Action |
| --- | --- |
| `T` | Focus element type bar |
| `D` | Focus dynamics bar |  
| `B` | Focus pitch bend bar |
| `+` | Select next-louder dynamic |
| `-` | Select next-quieter dynamic |
| `N` | Select natural-sign | 
| `F` | Select flat-sign |
| `S` | Select sharp-sign |  
| `Ctrl+O` | Open file |
| `Ctrl+S` | Save file |
| `Ctrl+N` | Create new file | 
| `SPACE` | Play/Pause/Resume |
| `Ctrl+P` | Create PDF Score |

#### Contextual keyboard shortcuts

These shortcuts trigger actions local to the currently selected element.

| Shortcut | Action |
| --- | --- |
| `Esc` | Abort element creation and go into select mode
| `Delete` |  Delete element |
| `⬇` | Move selected note head down / Select next element in focused bar |
| `⬆` | Move selected note head up / Select previous element in focused bar |
| `⬅`️ | Move selected element left / Select previous element in focused bar |
| `➡️` | Move selected element right / Select next element in focused bar |

### How to get it running

Being built on top of the JavaFX Framework, CoGnoSco should run on pretty much any OS. Just clone the project locally  
``git clone https://github.com/NKb03/CoGnoSco.git``  
and then build and run it using Gradle  
``./gradlew run``.  
To export graphic scores as ordinary PDF scores you need [LilyPond](https://lilypond.org/) on your path.

### Author

Nikolaus Knop ([niko.knop003@gmail.com](mailto:niko.knop003@gmail.com))

### License

This project is licensed under the GNU General Public License - see the [LICENSE](LICENSE) file for details.
