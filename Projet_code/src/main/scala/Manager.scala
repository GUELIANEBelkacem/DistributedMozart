package upmc.akka.leader
import akka.actor._
import javax.sound.midi._
import javax.sound.midi.ShortMessage._

import scala.util.Random

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
//import Musician._


class Manager (val id:Int, val terminaux:List[Terminal]) extends Actor {
    var activeCollegues:List[Int] = List()
    val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor") 
    val TIME_BASE = 50 milliseconds 
    val PRINT_TIME_BASE = 500 milliseconds
    val scheduler = context.system.scheduler
    val random = new Random

    var chief = -1

    def receive = {
        case Start =>{
               self ! Update
               self ! Looper
        }

        case Update =>{
         
            
            terminaux.foreach(n => {
                    if (n.id != id) {
                        
                        val remote = context.actorSelection("akka.tcp://MozartSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Musicien"+n.id)
                        remote ! Ping(id)
                    }
                
            })
            scheduler.scheduleOnce(TIME_BASE, self, Update)

        }

        case Ping(n)=>{
            //println("l gotta ping from "+n)
            if(!activeCollegues.contains(n)) this.activeCollegues = this.activeCollegues:::List(n)

        }

        case Looper =>{
            
            displayActor ! Message("alive collegues: "+activeCollegues)
            val tempList = activeCollegues:::List(id)
            if(!tempList.contains(chief)){
                chief = tempList.max
            }

            println("chief: "+chief)
            terminaux.foreach(n => {
                    if (n.id != id) {
                        
                        val remote = context.actorSelection("akka.tcp://MozartSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Musicien"+n.id)
                        remote ! Leader(chief)
                    }
                
            })
            this.activeCollegues = List()

            

            scheduler.scheduleOnce(PRINT_TIME_BASE, self, Looper)
        }

        case IsLeader(n:Int) =>{
               sender ! IsLeader(chief)
        }

        case Leader(n) => {
            if(n>chief){
                chief = n
            }
        }
        case GetPlayer(n) =>{
            sender ! GetPlayer(activeCollegues(random.nextInt(activeCollegues.length)))
        }
        
    }
}