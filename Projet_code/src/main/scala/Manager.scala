package upmc.akka.leader
import akka.actor._
import javax.sound.midi._
import javax.sound.midi.ShortMessage._

import scala.util.Random

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global

import scala.collection.immutable.ListMap
//import Musician._


class Manager (val id:Int, val terminaux:List[Terminal]) extends Actor {
    var activeCollegues:ListMap[Int,Int] = ListMap()
    val displayActor = context.actorOf(Props[DisplayActor], name = "displayActor") 
    val TIME_BASE = 50 milliseconds 
    val PRINT_TIME_BASE = 500 milliseconds
    val COUNT_TIME_BASE = 1000 milliseconds
    val scheduler = context.system.scheduler
    val random = new Random

    var chief = -1
    var chiefage = -1
    var count = 0
    var lcount = 0
    var killme = true
    def receive = {
        case Start =>{
               self ! Update
               self ! Looper
               self ! Counter
        }

        case Update =>{
         
            
            terminaux.foreach(n => {
                    if (n.id != id) {
                        
                        val remote = context.actorSelection("akka.tcp://MozartSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Musicien"+n.id)
                        remote ! Ping(id, count)
                    }
                
            })
            scheduler.scheduleOnce(TIME_BASE, self, Update)

        }

        case Ping(n, m)=>{
            //println("l gotta ping from "+n)
            if(chief == n) {chiefage = m}
            if(!activeCollegues.contains(n)){
                this.activeCollegues = this.activeCollegues ++ ListMap(n -> m)
                killme = false
                
            }

        }

        case Looper =>{
            
            displayActor ! Message("alive collegues: "+activeCollegues)
            val tempList = activeCollegues ++ ListMap(id -> count)
            if(!tempList.contains(chief)){
                ListMap(tempList.toSeq.sortBy(_._1):_*)
                println("ELECTIOOOOOOOOOOOOOOOOOOOOOOOOOOOOOON:   "+tempList)
                var newn = -1
                var newm = -1
                tempList.keys.foreach{ i =>
                    if(tempList(i) > newm){
                        newn = i
                        newm = tempList(i)
                    }
                }
                
                chief = newn
                chiefage = newm
            }

            println("chief: "+chief)
            terminaux.foreach(n => {
                    if (n.id != id) {
                        
                        val remote = context.actorSelection("akka.tcp://MozartSystem" + n.id + "@" + n.ip + ":" + n.port + "/user/Musicien"+n.id)
                        remote ! Leader(chief, chiefage)
                    }
                
            })
            this.activeCollegues = ListMap()

            

            scheduler.scheduleOnce(PRINT_TIME_BASE, self, Looper)
        }

        case IsLeader(n:Int) =>{
               sender ! IsLeader(chief)
        }

        case Leader(n, m) => {
            if(m>chiefage){
                chief = n
            }
        }
        case GetPlayer(n) =>{
            val keys = activeCollegues.keys.toList
            if(activeCollegues.size != 0){sender ! GetPlayer(keys(random.nextInt(activeCollegues.size)))}
            
        }

        case Counter =>{
            count = count + 1
            if(id == chief){
                lcount = lcount +1
                if((lcount%20) == 0 ) {
                    if(killme){
                        println("NO ONE CAME :'( ")
                        System.exit(0)
                    }
                    lcount=0
                    killme = true
                }
            }
            scheduler.scheduleOnce(COUNT_TIME_BASE, self, Counter)
        }
        
    }
}