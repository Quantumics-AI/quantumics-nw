import { Component, OnInit, EventEmitter, Output, Input } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, Subject } from 'rxjs';
import { Certificate } from 'src/app/models/certificate';
import { Quantumfacade } from 'src/app/state/quantum.facade';
import { takeUntil } from 'rxjs/operators';
import { SourceDataService } from '../../services/source-data.service';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { AwsConfirmationComponent } from '../aws-confirmation/aws-confirmation.component';
import { EditDataSourceComponent } from '../edit-data-source/edit-data-source.component';

@Component({
  selector: 'app-list-aws',
  templateUrl: './list-aws.component.html',
  styleUrls: ['./list-aws.component.css']
})
export class ListAwsComponent {

  private certificate$: Observable<Certificate>;
  private unsubscribe: Subject<void> = new Subject();
  private certificateData: Certificate;

  projectId: string;
  userId: number;
  loading: boolean;

  searchTerm: any = { name: '' };
  public searchDiv: boolean = false;
  public searchString: string;
  public awsListData = [
    {id:1, name : "AWS1", access_type: "IAM", role: "user", createdDate: "08-08-23, 02:08 PM", modifiedDate: '' },
    {id:2, name : "AWS2", access_type: "IAM", role: "user", createdDate: "08-08-23, 02:08 PM", modifiedDate: '' },
    {id:3, name : "AWS3", access_type: "IAM", role: "user", createdDate: "08-08-23, 02:08 PM", modifiedDate: '' },
    {id:4, name : "AWS4", access_type: "IAM", role: "user", createdDate: "08-08-23, 02:08 PM", modifiedDate: '' },
    {id:5, name : "AWS5", access_type: "IAM", role: "user", createdDate: "08-08-23, 02:08 PM", modifiedDate: '' }
  ];

  constructor(
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private modalService: NgbModal,
    private quantumFacade: Quantumfacade,
    private sourceDataService: SourceDataService,
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
    this.projectId = localStorage.getItem('project_id');
  }

  public edit(d: any): void {
    const modalRef = this.modalService.open(EditDataSourceComponent, { size: 'md', scrollable: false });
    modalRef.componentInstance.userId = this.userId;
    modalRef.componentInstance.projectId = this.projectId;
    modalRef.componentInstance.awsId = d.id;
    modalRef.componentInstance.sourceName = d.name;
    modalRef.componentInstance.accessType = d.access_type;
    modalRef.componentInstance.role = d.role;


    modalRef.result.then((result) => {
      
    }, (result) => {
      
     });
  }

  public delete(id:number): void {
    const modalRef = this.modalService.open(AwsConfirmationComponent, { size: 'md modal-dialog-centered', scrollable: false });
    modalRef.componentInstance.userId = this.userId;
    modalRef.componentInstance.projectId = this.projectId;
    modalRef.componentInstance.awsId = id;

    modalRef.result.then((result) => {
      this.loading = true;
      const idx = this.awsListData.findIndex(x => x.id === id);
      this.awsListData.splice(idx, 1);
      
    }, (result) => {
      
     });
  }

  searchInput(str) {
    this.searchString = str;
    if (str.length == 0) {
      this.searchDiv = false;
    } else {
      this.searchDiv = true;
    }
  }

  clearSearhInput() {
    this.searchTerm = { folderName: '' };
    this.searchDiv = false;
  }
}
