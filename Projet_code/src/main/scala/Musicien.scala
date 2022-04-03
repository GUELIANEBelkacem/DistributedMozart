package upmc.akka.leader

import akka.actor._
//import akka.remote._
//import math._

import javax.sound.midi._
import javax.sound.midi.ShortMessage._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

//import scala.concurrent._




case class Start ()
case class Update ()
case class Ping(n:Int)
case class Looper()
case class IsLeader(chief:Int)
case class Leader(chief:Int)
case class GetPlayer(who:Int)


class Musicien (val id:Int, val terminaux:List[Terminal]) extends Actor {

     import PlayerActor._
     import DataBaseActor._
     // Les differents acteurs du systeme
     val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor")
     val managerActor = context.actorOf(Props(new Manager(id, terminaux)), name = "LM")
     val instrument = context.actorOf(Props[Piano], name = "piano")
     
     
     var isChief = false;


     val TIME_BASE = 1000 milliseconds
     val LOOPER_TIME_BASE = 1800 milliseconds
     val scheduler = context.system.scheduler


     var collegues:List[ActorSelection] = List()
     def receive = {

          // Initialisation
          case Start => {
               //instrument ! (Measure(List(Chord (0 , List (Note(42 ,610, 86), Note(54 ,594, 81), Note(81 ,315, 96))),Chord  (292 , List (Note(78 ,370, 78) ) ) ) ) )
               displayActor ! Message ("Musicien " + this.id + " is created")
               managerActor ! Start
               self ! Update
               self ! Looper
          }
          case Measure(l) =>{
               //instrument ! Measure(l)
               println("PLAYING!!!!!!!!!!!!!!!!!!!!")
          }

          case Update =>{
               managerActor ! IsLeader(id)
               scheduler.scheduleOnce(TIME_BASE, self, Update)
          }

          case Ping(n) =>{
               //println("gotta ping from "+n)
               managerActor ! Ping(n)
          }

          case IsLeader(b) =>{
               isChief = (b==id)
          }

          case Leader(n) => {
               managerActor ! Leader(n)
          }
          case Looper=>{
               println("am i chief: "+isChief )
               if(isChief){ managerActor ! GetPlayer(id)}
               scheduler.scheduleOnce(LOOPER_TIME_BASE, self, Looper)
          }
          case GetPlayer(m) =>{
            terminaux.foreach(n => {
                    if (n.id == m) {
                        
                        val remote = context.actorSelection("akka.tcp://MozartSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Musicien"+n.id)
                        remote ! Measure(List())
                    }
                
            })
          }

     }
}


class Piano () extends Actor{
  import DataBaseActor._
  import PlayerActor._
  device.open()


  def receive = {
    case Measure (l) => {
      //println("jouer ici une measure")
      for (c <- l) {
        c match{
          case Chord(e, lc) => 
            //var del = e
            for(n <- lc){
            self ! MidiNote(n.pitch, n.vol, n.dur, e)
          }
          case _ =>
        }
      }
    }
    case MidiNote(p,v, d, at) => {
      context.system.scheduler.scheduleOnce ((at) milliseconds) (note_on (p,v,10))
      context.system.scheduler.scheduleOnce ((at+d) milliseconds) (note_off (p,10))
    }
  }
}


object PlayerActor {
  case class MidiNote (pitch:Int, vel:Int, dur:Int, at:Int) 
  val info = MidiSystem.getMidiDeviceInfo().filter(_.getName == "Gervill").headOption
  // or "SimpleSynth virtual input" or "Gervill"
     val device = info.map(MidiSystem.getMidiDevice).getOrElse {
     println("[ERROR] Could not find Gervill synthesizer.")
     sys.exit(1)
     }

     val rcvr = device.getReceiver()

/////////////////////////////////////////////////
     def note_on (pitch:Int, vel:Int, chan:Int): Unit = {
     val msg = new ShortMessage
     msg.setMessage(NOTE_ON, chan, pitch, vel)
     rcvr.send(msg, -1)
     }

     def note_off (pitch:Int, chan:Int): Unit = {
     val msg = new ShortMessage
     msg.setMessage(NOTE_ON, chan, pitch, 0)
     rcvr.send(msg, -1)
     }

}

object DataBaseActor {
	abstract class ObjetMusical
 	case class Note (pitch:Int, dur:Int, vol:Int) extends ObjetMusical
 	case class Chord (date:Int, notes:List[Note]) extends ObjetMusical
 	case class Measure (chords:List[Chord]) extends ObjetMusical
 
 	case class GetMeasure (num:Int)
 	//case class Start()
}