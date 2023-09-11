import { Component, OnInit, EventEmitter, Output, Input } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject } from 'rxjs';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { retry, takeUntil } from 'rxjs/operators';
import { SnackbarService } from 'src/app/core/services/snackbar.service';

@Component({
  selector: 'app-browse-file',
  templateUrl: './browse-file.component.html',
  styleUrls: ['./browse-file.component.scss']
})
export class BrowseFileComponent implements OnInit {

  loading: boolean;
  public isDescending: boolean;
  projectId: string;
  userId: number;
  private certificate$: Observable<Certificate>;
  private unsubscribe: Subject<void> = new Subject();
  private certificateData: Certificate;

  public tableData = [
    {
      name: "Root",
      type: "folder",
      id: 1,
      createdDate: new Date(),
      expanded: false,
      children: [{
          name: "Folder 1",
          type: "folder",
          childid: 1,
          createdDate: new Date(),
          children: [{
              name: "File 1",
              type: "file",
              subid: 1,
              createdDate: new Date(),
            },
            {
              name: "Folder 1.1",
              type: "folder",
              subid: 2,
              createdDate: new Date(),
              children: [{
                name: "File 1.1.1",
                type: "file",
                subid: 1,
                createdDate: new Date()
              }]
            }
          ]
        },
        {
          name: "File 2",
          type: "file",
          childid: 2,
          createdDate: new Date(),
        }
      ]
    },
    {
      name: "Root2",
      type: "folder",
      id: 2,
      createdDate: new Date(),
      expanded: false,
      children: [{
          name: "Folder 2",
          type: "folder",
          children: [{
              name: "File 2",
              type: "file",
              childid: 1,
              createdDate: new Date(),
            },
            {
              name: "Folder 1.1",
              type: "folder",
              childid: 2,
              createdDate: new Date(),
              children: [{
                name: "File 1.1.1",
                type: "file"
              }]
            }
          ]
        },
        {
          name: "File 2",
          type: "file",
          fileid: 2,
          createdDate: new Date(),
        }
      ]
    }
  ];

  public childData: any;


  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private modalService: NgbModal,
    private quantumFacade: Quantumfacade,
    private snakbar: SnackbarService
  ){
    this.certificate$ = this.quantumFacade.certificate$;
    this.certificate$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(certificate => {
        if (certificate) {
          this.certificateData = certificate;
          this.userId = +this.certificateData.user_id;
        }
      });
  }

  ngOnInit(): void {
    // getPipelineData
    this.projectId = localStorage.getItem('project_id');
  }

  public getTableFile(d: any): void {
    this.tableData.map(t => {
      if(t.id == d.id){
        t.expanded = !t.expanded;
        console.log("expanded data:",t);
        this.childData = t.children;
        console.log("expanded child:",this.childData);
      } else {
        t.expanded = false;
      }
    })
  }
}
