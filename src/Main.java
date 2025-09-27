import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

// ======================= MODEL =======================
class CharacterStats {
    private int[] stats;
    public CharacterStats() { stats = generate(); }
    private int[] generate() {
        Random r = new Random();
        int[] arr = new int[6];
        for (int i = 0; i < 6; i++) {
            int[] rolls = new int[6];
            for (int j = 0; j < 6; j++) rolls[j] = r.nextInt(6) + 1;
            Arrays.sort(rolls);
            arr[i] = rolls[3] + rolls[4] + rolls[5];
        }
        return arr;
    }
    public void reroll() { stats = generate(); }
    public int[] getStats() { return stats; }
    public Memento save() { return new Memento(stats.clone()); }
    public void restore(Memento m) { stats = m.getState(); }

    static class Memento {
        private final int[] state;
        private Memento(int[] s) { state = s; }
        private int[] getState() { return state; }
    }
}

interface CharacterClass {
    String getName();
    int baseHp();
    void specialSkill();
    void accept(CharacterVisitor v);
}
class Paladin implements CharacterClass {
    public String getName(){ return "Paladin"; }
    public int baseHp(){ return 10; }
    public void specialSkill(){ }
    public void accept(CharacterVisitor v){ v.visit(this); }
}
class Rogue implements CharacterClass {
    public String getName(){ return "Rogue"; }
    public int baseHp(){ return 8; }
    public void specialSkill(){ }
    public void accept(CharacterVisitor v){ v.visit(this); }
}

interface CharacterRace {
    String getName();
    void applyBonuses(int[] stats);
    void accept(CharacterVisitor v);
}
class Halfling implements CharacterRace {
    public String getName(){ return "Halfling"; }
    public void applyBonuses(int[] s){ s[1]+=2; }
    public void accept(CharacterVisitor v){ v.visit(this); }
}
class Goblin implements CharacterRace {
    public String getName(){ return "Goblin"; }
    public void applyBonuses(int[] s){ s[0]+=1; s[1]+=2; }
    public void accept(CharacterVisitor v){ v.visit(this); }
}

// Visitor
interface CharacterVisitor {
    void visit(Paladin p);
    void visit(Rogue r);
    void visit(Halfling h);
    void visit(Goblin g);
}
class InfoPrinterVisitor implements CharacterVisitor {
    public void visit(Paladin p){ System.out.println("Visitor: Paladin!"); }
    public void visit(Rogue r){ System.out.println("Visitor: Rogue!"); }
    public void visit(Halfling h){ System.out.println("Visitor: Halfling!"); }
    public void visit(Goblin g){ System.out.println("Visitor: Goblin!"); }
}

// Model container
class CharacterModel {
    private CharacterStats stats = new CharacterStats();
    private CharacterClass clazz;
    private CharacterRace race;
    private final java.util.List<String> history = new ArrayList<>();
    private CharacterStats.Memento saved;

    public void setClass(CharacterClass c){ this.clazz = c; }
    public void setRace(CharacterRace r){ this.race = r; }
    public void reroll(){ stats.reroll(); }
    public void save(){ saved = stats.save(); }
    public void restore(){ if(saved!=null) stats.restore(saved); }
    public int[] getStats(){ return stats.getStats(); }
    public CharacterClass getClazz(){ return clazz; }
    public CharacterRace getRace(){ return race; }
    public void addHistory(String h){ history.add(h); }
    public java.util.List<String> getHistory(){ return history; }
}

// ======================= VIEW =======================
class CharacterView extends JFrame {
    JComboBox<String> classBox = new JComboBox<>(new String[]{"Paladin","Rogue"});
    JComboBox<String> raceBox  = new JComboBox<>(new String[]{"Halfling","Goblin"});
    JButton btnReroll = new JButton("Reroll Stats");
    JButton btnSave = new JButton("Save State");
    JButton btnRestore = new JButton("Restore State");
    JButton btnAdd = new JButton("Add Character");
    JTextArea output = new JTextArea(10,40);

    public CharacterView(){
        super("DnD Character Generator (MVC)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        JPanel top = new JPanel();
        top.add(new JLabel("Class:")); top.add(classBox);
        top.add(new JLabel("Race:"));  top.add(raceBox);
        top.add(btnReroll); top.add(btnSave);
        top.add(btnRestore); top.add(btnAdd);
        add(top,BorderLayout.NORTH);
        output.setEditable(false);
        add(new JScrollPane(output),BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }
}

// ======================= CONTROLLER =======================
class CharacterController {
    private final CharacterModel model;
    private final CharacterView view;

    CharacterController(CharacterModel m, CharacterView v){
        model=m; view=v;
        init();
    }

    private void init(){
        view.btnReroll.addActionListener(e->{
            model.reroll();
            showStats();
        });
        view.btnSave.addActionListener(e->{
            model.save();
            view.output.append("State saved!\n");
        });
        view.btnRestore.addActionListener(e->{
            model.restore();
            showStats();
        });
        view.btnAdd.addActionListener(e->addCharacter());
    }

    private void showStats(){
        view.output.append("Stats: "+Arrays.toString(model.getStats())+"\n");
    }

    private void addCharacter(){
        String cName = (String)view.classBox.getSelectedItem();
        String rName = (String)view.raceBox.getSelectedItem();
        CharacterClass clazz = cName.equals("Paladin") ? new Paladin() : new Rogue();
        CharacterRace race   = rName.equals("Halfling")? new Halfling(): new Goblin();
        model.setClass(clazz);
        model.setRace(race);

        // Apply racial bonuses
        int[] finalStats = model.getStats().clone();
        race.applyBonuses(finalStats);

        // Visitor demo
        InfoPrinterVisitor v = new InfoPrinterVisitor();
        clazz.accept(v);
        race.accept(v);

        String rec = "Class:"+clazz.getName()+" Race:"+race.getName()+
                " Stats:"+Arrays.toString(finalStats);
        model.addHistory(rec);
        view.output.append(rec+"\n");

        // Save to JSON
        saveToJson("characters.json", model.getHistory());
    }

    private void saveToJson(String file, java.util.List<String> list){
        StringBuilder sb=new StringBuilder();
        sb.append("{\n  \"characters\":[\n");
        for(int i=0;i<list.size();i++){
            sb.append("    { \"info\": \"").append(list.get(i)).append("\" }");
            if(i<list.size()-1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}");
        try(FileWriter fw = new FileWriter(file)){
            fw.write(sb.toString());
        }catch(IOException ex){ ex.printStackTrace(); }
    }
}

// ======================= MAIN =======================
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(()->{
            CharacterModel m = new CharacterModel();
            CharacterView  v = new CharacterView();
            new CharacterController(m,v);
            v.setVisible(true);
        });
    }
}
