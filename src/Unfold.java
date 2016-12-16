import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

import Jcg.geometry.*;
import Jcg.graph.Graph;
import Jcg.graph.Node;
import Jcg.mesh.MeshLoader;
import Jcg.polyhedron.*;



public class Unfold {
	
	
	private String filename;
	Polyhedron_3<Point_3> polyhedron3D; // triangle mesh

	

	
//	class DualTree{
//		class DualNode{
//			Face<Point_3> f;
//			HashSet<DualNode> neighbours;
//			
//			DualNode(Face<Point_3> f){
//				this.f=f;
//				this.neighbours=new HashSet<DualNode>();
//				
//			}
//			
//			void addNeighbour(DualNode n){
//				this.neighbours.add(n);
//			}
//		}
//		
//		DualNode root;
//		public DualTree(DualNode root){
//			this.root=root;
//			
//		}
//	}
	
	
	
	public Unfold(String filename){
		this.filename=filename;
		this.polyhedron3D=MeshLoader.getSurfaceMesh(filename);
		
	}
	
	
	
	public HashSet<Halfedge<Point_3>> outgoingEdgesFrom(Vertex<Point_3> v){
		HashSet<Halfedge<Point_3>> out=new HashSet<Halfedge<Point_3>>();
		for(Halfedge<Point_3> e=v.getHalfedge().getOpposite();!out.contains(e);e=e.getOpposite().getNext()){
			out.add(e);
		}
		return out;
	}
	
	public HashSet<Halfedge<Point_3>> dfs(){
		HashSet<Halfedge<Point_3>> cut=new HashSet<Halfedge<Point_3>>();
		HashSet<Vertex<Point_3>> visited=new HashSet<Vertex<Point_3>>() ;
		Vertex<Point_3> v=this.getVertex(0);
		dfsAux(v,visited,cut);
		return cut;
	}
	public void dfsAux(Vertex<Point_3> v,HashSet<Vertex<Point_3>> visited,HashSet<Halfedge<Point_3>> cut){
		visited.add(v);
		for(Halfedge<Point_3> e:this.outgoingEdgesFrom(v)){
			Vertex<Point_3> w=e.vertex;
			if(!visited.contains(w)){
				cut.add(e);
				dfsAux(w,visited,cut);
			}
		}
	}
	//now we have a dual tree represented by the hash map which maps each face to its neighbours
	public HashMap<Face<Point_3>,HashSet<Face<Point_3>>> toJoin(HashSet<Halfedge<Point_3>> cut){
		HashMap<Face<Point_3>,HashSet<Face<Point_3>>> dTree=new HashMap<Face<Point_3>,HashSet<Face<Point_3>>>();
		
		for(Face<Point_3> f:this.polyhedron3D.facets){
			dTree.put(f, new HashSet<Face<Point_3>>());
			
		}
		for(Halfedge<Point_3> e:this.polyhedron3D.halfedges){
			if((!cut.contains(e))&&(!cut.contains(e.getOpposite()))){
				Face<Point_3> f=e.getFace();
				HashSet<Face<Point_3>> n=dTree.get(f);
				n.add(e.getOpposite().getFace());
				dTree.put(f, n);
			}
		}
//		for(Face<Point_3> f:dTree.keySet()){
////			for(Face<Point_3> f1:dTree.get(f)){
////				System.out.println(this.getCommonEdge(f, f1));
////				
////			}
////			System.out.println();
////		}
		return dTree;
	}
	
	//result belongs to first argument
	public Halfedge<Point_3> getCommonEdge(Face<Point_3> f1, Face<Point_3> f2){
		Halfedge<Point_3> e1=f1.getEdge();
		while(e1.getOpposite().getFace()!=f2){
			e1=e1.getNext();
		}
		return e1;
	}
	
	public void toPlan(HashMap<Face<Point_3>,HashSet<Face<Point_3>>> dTree){
		HashMap<Integer, Point_2> hPoint=new HashMap<Integer, Point_2>();
		HashSet<LinkedList<Integer>> hFace=new HashSet<LinkedList<Integer>>();
		HashMap<Halfedge<Point_3>,int[]> hEdge=new HashMap<Halfedge<Point_3>, int[]>();
		HashSet<Face<Point_3>> visited=new HashSet<Face<Point_3>>();
		HashMap<Face<Point_3>,HashSet<Face<Point_3>>> joinTree=this.toJoin(this.dfs());
		//Embed the first face on the dual cut tree
		Face<Point_3> f0=this.getFace(0);
		visited.add(f0);
		
		Halfedge<Point_3> e0=f0.getEdge();
		hPoint.put(0, new Point_2(0,0));
		hPoint.put(hPoint.size(),new Point_2(Math.sqrt(toVector3(e0).squaredLength().doubleValue()),0));
		hEdge.put(e0, new int[]{1,0});
		
		
		
		Embed(f0,hPoint,hFace,hEdge);
		traverseFaceFrom(f0,hPoint,hFace,hEdge,visited);
		//write the output based on hPoint and hFace
		for(int i=0;i<hPoint.size();i++){
			System.out.println(hPoint.get(i));
		}
		System.out.println();
		for(LinkedList<Integer> list:hFace){
			for(int i:list){
				System.out.println(i);
			}
			System.out.println();
		}
		
		return;
	}
	public Vector_3 toVector3(Halfedge<Point_3> e){
		return new Vector_3(e.getOpposite().getVertex().getPoint(),e.getVertex().getPoint());
	}
	
	public Vector_2 rotateVector2(Vector_2 v, double sin, double cos){
		double x0,y0,x1,y1;
		x0=v.getX().doubleValue();
		y0=v.getY().doubleValue();
		x1=cos*x0-sin*y0;
		y1=sin*x0+cos*y0;
		return new Vector_2(x1,y1);
	}
	
	//to be corrected
	public void Embed(Face<Point_3> f0,HashMap<Integer, Point_2> hPoint, HashSet<LinkedList<Integer>> hFace, HashMap<Halfedge<Point_3>,int[]> hEdge){
		LinkedList<Integer> curFace=new LinkedList<Integer>();//a linkedlist representing the current face being treated
		Halfedge<Point_3> e0=f0.getEdge();
		hPoint.put(0, new Point_2(0,0));
		curFace.add(0);
		hPoint.put(hPoint.size(),new Point_2(Math.sqrt(toVector3(e0).squaredLength().doubleValue()),0));
		curFace.add(1);
		hEdge.put(e0, new int[]{1,0});
		Vector_3 v1,v2;
		Vector_2 r,vNew;
		double cos,sin;
		Halfedge<Point_3> e1=e0.getNext();
		while(e1.getNext()!=e0){
			v1=toVector3(e1.getPrev());
			v2=toVector3(e1);
			cos=v1.innerProduct(v2).doubleValue()/Math.sqrt(v1.squaredLength().doubleValue()*v2.squaredLength().doubleValue());
			sin=Math.sqrt(v1.crossProduct(v2).squaredLength().doubleValue())/Math.sqrt(v1.squaredLength().doubleValue()*v2.squaredLength().doubleValue());
			r=new Vector_2(new Point_2(0,0),hPoint.get(hPoint.size()-1));
			vNew=new Vector_2(hPoint.get(hPoint.size()-2),hPoint.get(hPoint.size()-1));
			vNew=rotateVector2(vNew,sin,cos).multiplyByScalar(Math.sqrt(v2.squaredLength().doubleValue())).divisionByScalar(Math.sqrt(vNew.squaredLength().doubleValue()));
			r=r.sum(vNew);
			hPoint.put(hPoint.size(), new Point_2(r.getX(),r.getY()));
			hEdge.put(e1,new int[]{hPoint.size()-2,hPoint.size()-1});
			curFace.add(hPoint.size()-1);
			e1=e1.getNext();
		}
		hFace.add(curFace);
		return;
	}
	
	//to be corrected
	public void addFace(Face<Point_3> f,Halfedge<Point_3> e,HashMap<Integer, Point_2> hPoint, HashSet<LinkedList<Integer>> hFace, HashMap<Halfedge<Point_3>,int[]> hEdge,HashSet<Face<Point_3>> visited){
		LinkedList<Integer> curFace=new LinkedList<Integer>();//a linkedlist representing the current face being treated
//		Halfedge<Point_3> e0=f.getEdge();
//		hPoint.put(0, new Point_2(0,0));
//		curFace.add(0);
//		hPoint.put(hPoint.size(),new Point_2(Math.sqrt(toVector3(e0).squaredLength().doubleValue()),0));
//		curFace.add(1);
//		hEdge.put(e0, new int[]{1,0});
		int[] startingEdge=hEdge.get(e);
		for(int i:startingEdge){
			curFace.add(i);
		}
		int p1=startingEdge[0];
		int p2=startingEdge[1];
		curFace.add(p1);
		curFace.add(p2);
		
		
		
		int flag=0;
		Vector_3 v1,v2;
		Vector_2 r,vNew;
		double cos,sin;
		Halfedge<Point_3> e1=e.getNext();
		while(e1.getNext()!=e){
			flag++;
			v1=toVector3(e);
			v2=new Vector_3(e.getVertex().getPoint(),e1.getVertex().getPoint());
			cos=v1.innerProduct(v2).doubleValue()/Math.sqrt(v1.squaredLength().doubleValue()*v2.squaredLength().doubleValue());
			sin=Math.sqrt(v1.crossProduct(v2).squaredLength().doubleValue())/Math.sqrt(v1.squaredLength().doubleValue()*v2.squaredLength().doubleValue());
			r=new Vector_2(new Point_2(0,0),hPoint.get(p2));
			vNew=new Vector_2(hPoint.get(p1),hPoint.get(p2));
			vNew=rotateVector2(vNew,sin,cos).multiplyByScalar(Math.sqrt(v2.squaredLength().doubleValue())).divisionByScalar(Math.sqrt(vNew.squaredLength().doubleValue()));
			r=r.sum(vNew);
			hPoint.put(hPoint.size(), new Point_2(r.getX(),r.getY()));
			if(flag>0){
				hEdge.put(e1,new int[]{hPoint.size()-1,hPoint.size()-2});
			}else{
				hEdge.put(e1, new int[]{});//to be corrected
			}
			curFace.add(hPoint.size()-1);
			e1=e1.getNext();
		}
		hFace.add(curFace);
		return;
	}
	
	
	public void traverseFaceFrom(Face<Point_3> f0,HashMap<Integer, Point_2> hPoint, HashSet<LinkedList<Integer>> hFace, HashMap<Halfedge<Point_3>,int[]> hEdge,HashSet<Face<Point_3>> visited){
		return;
	}

	
	
	public void show3D(){
		Show3DMesh.main(new String[]{filename});

	}
	
	public void show2D(){
		ShowPlanarUnfolding.main(new String[]{filename});

	}
	
	public Vertex<Point_3> getVertex(int i){
		return polyhedron3D.vertices.get(i);
	}
	
	public Face<Point_3> getFace(int i){
		return polyhedron3D.facets.get(i);
	}
	
	
	public void testOutgoing(int i){
		Vertex<Point_3> v=getVertex(i);
		HashSet<Halfedge<Point_3>> test=outgoingEdgesFrom(v);
		System.out.println(v);
		for(Halfedge<Point_3> e:test){
			System.out.println(e);
		}
	}
	
	public void testDfs(){
		HashSet<Halfedge<Point_3>> test=this.dfs();
		for(Halfedge<Point_3> e:test){
			System.out.println(e);
		}
	}
	public static void main(String[] args){
		Unfold u=new Unfold("cube.off");
		//u.testOutgoing(0);
		//u.testDfs();
		//u.show3D();
		u.toPlan(u.toJoin(u.dfs()));
	//	HashSet<Halfedge<Point_3>> s=outgoingEdgesFrom();
		return;
	}
}
